/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoBodyIndex
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLoopBase
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite.newInstance
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.backend.common.serialization.proto.Annotations as ProtoAnnotations
import org.jetbrains.kotlin.backend.common.serialization.proto.DescriptorReference as ProtoDescriptorReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoSymbolIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSymbolData as ProtoSymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSymbolKind as ProtoSymbolKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoTypeIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoStringIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression

abstract class KotlinIrLinker(
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    private val firstKnownBuiltinsIndex: Long
) : DescriptorUniqIdAware, IrDeserializer {


    sealed class DeserializationState {
        val deserializedSymbols = mutableMapOf<UniqId, IrSymbol>()
        val reachableTopLevels = mutableSetOf<UniqId>()
        val deserializedTopLevels = mutableSetOf<UniqId>()

        operator fun contains(key: UniqId) = key in deserializedSymbols
        operator fun get(key: UniqId): IrSymbol = deserializedSymbols[key] ?: error("No deserialized symbol found for $key")

        abstract fun addUniqID(key: UniqId)
        protected open fun peekReachableKey(): UniqId? = reachableTopLevels.firstOrNull()

        class ModuleDeserializationState(val module: IrModuleDeserializer): DeserializationState() {
            override fun addUniqID(key: UniqId) {
                module.addModuleReachableTopLevel(key)
            }
        }

        class SimpleDeserializationState: DeserializationState() {
            override fun addUniqID(key: UniqId) {
                reachableTopLevels.add(key)
            }

            override fun peekReachableKey(): UniqId {
                return reachableTopLevels.firstOrNull() ?: error("Expecting non-empty set")
            }
        }

        fun processPendingDeclaration(processor: (UniqId) -> Unit) {
            do {

                val reachableKey = peekReachableKey() ?: return

                if (deserializedSymbols[reachableKey]?.isBound == true) {
                    reachableTopLevels.remove(reachableKey)
                    deserializedTopLevels.add(reachableKey)
                    continue
                }

                processor(reachableKey)

                reachableTopLevels.remove(reachableKey)
                deserializedTopLevels.add(reachableKey)

            } while (reachableTopLevels.isNotEmpty())
        }
    }

    protected val globalDeserializationState = DeserializationState.SimpleDeserializationState()
    private val modulesWithReachableTopLevels = mutableSetOf<IrModuleDeserializer>()

    //TODO: This is Native specific. Eliminate me.
    private val forwardDeclarations = mutableSetOf<IrSymbol>()
    val resolvedForwardDeclarations = mutableMapOf<UniqId, UniqId>()

    protected val deserializersForModules = mutableMapOf<ModuleDescriptor, IrModuleDeserializer>()

    private fun getForwardDeclararationModuleDeserializer() = deserializersForModules.entries.single { it.key.isForwardDeclarationModule }.value

    inner class IrModuleDeserializer(
        private val moduleDescriptor: ModuleDescriptor,
        private val deserializationStrategy: DeserializationStrategy
    ) {

        val fileToDeserializerMap = mutableMapOf<IrFile, IrDeserializerForFile>()

        protected val moduleResolvedForwardDeclarations = mutableMapOf<UniqId, UniqId>()

        private val moduleDeserializationState = DeserializationState.ModuleDeserializationState(this)
        private val moduleReversedFileIndex = mutableMapOf<UniqId, IrDeserializerForFile>()
        private val moduleDependencies by lazy {
            moduleDescriptor.allDependencyModules.filter { it != moduleDescriptor }.map { deserializersForModules[it]!! }
        }

        // This is a heavy initializer
        val module = deserializeIrModuleHeader()

        inner class IrDeserializerForFile(private var annotationsProto: ProtoAnnotations?, private val fileIndex: Int, onlyHeaders: Boolean) :
            IrFileDeserializer(logger, builtIns, symbolTable) {

            private var fileLoops = mutableMapOf<Int, IrLoopBase>()

            private val symbolProtosCache = mutableMapOf<Int, ProtoSymbolData>()
            private val typeProtosCache = mutableMapOf<Int, ProtoType>()
            private val stringsCache = mutableMapOf<Int, String>()

            lateinit var file: IrFile

            private val deserializeBodies: Boolean = !onlyHeaders

            private val fileLocalResolvedForwardDeclarations = mutableMapOf<UniqId, UniqId>()

            val fileLocalDeserializationState = DeserializationState.SimpleDeserializationState()

            fun deserializeDeclaration(key: UniqId): IrDeclaration {
                return deserializeDeclaration(loadTopLevelDeclarationProto(key), file)
            }

            private fun loadTopLevelDeclarationProto(uniqId: UniqId): ProtoDeclaration {
                val stream = reader(moduleDescriptor, fileIndex, uniqId).codedInputStream
                return ProtoDeclaration.parseFrom(stream, newInstance())
            }

            private fun loadSymbolProto(index: Int): ProtoSymbolData {
                val stream = readSymbol(moduleDescriptor, fileIndex, index).codedInputStream
                return ProtoSymbolData.parseFrom(stream, newInstance())
            }

            private fun loadTypeProto(index: Int): ProtoType {
                val stream = readType(moduleDescriptor, fileIndex, index).codedInputStream
                return ProtoType.parseFrom(stream, newInstance())
            }

            private fun loadStatementBodyProto(index: Int): ProtoStatement {
                val stream = readBody(moduleDescriptor, fileIndex, index).codedInputStream
                return ProtoStatement.parseFrom(stream, newInstance())
            }

            private fun loadExpressionBodyProto(index: Int): ProtoExpression {
                val stream = readBody(moduleDescriptor, fileIndex, index).codedInputStream
                return ProtoExpression.parseFrom(stream, newInstance())
            }

            private fun loadStringProto(index: Int): String {
                return String(readString(moduleDescriptor, fileIndex, index))
            }

            private fun referenceDeserializedSymbol(
                proto: ProtoSymbolData,
                descriptor: DeclarationDescriptor?
            ): IrSymbol = when (proto.kind) {
                ProtoSymbolKind.ANONYMOUS_INIT_SYMBOL ->
                    IrAnonymousInitializerSymbolImpl(
                        descriptor as ClassDescriptor?
                            ?: WrappedClassDescriptor()
                    )
                ProtoSymbolKind.CLASS_SYMBOL ->
                    symbolTable.referenceClass(
                        descriptor as ClassDescriptor?
                            ?: WrappedClassDescriptor()
                    )
                ProtoSymbolKind.CONSTRUCTOR_SYMBOL ->
                    symbolTable.referenceConstructor(
                        descriptor as ClassConstructorDescriptor?
                            ?: WrappedClassConstructorDescriptor()
                    )
                ProtoSymbolKind.TYPE_PARAMETER_SYMBOL ->
                    symbolTable.referenceTypeParameter(
                        descriptor as TypeParameterDescriptor?
                            ?: WrappedTypeParameterDescriptor()
                    )
                ProtoSymbolKind.ENUM_ENTRY_SYMBOL ->
                    symbolTable.referenceEnumEntry(
                        descriptor as ClassDescriptor?
                            ?: WrappedEnumEntryDescriptor()
                    )
                ProtoSymbolKind.STANDALONE_FIELD_SYMBOL ->
                    symbolTable.referenceField(WrappedFieldDescriptor())

                ProtoSymbolKind.FIELD_SYMBOL ->
                    symbolTable.referenceField(
                        descriptor as PropertyDescriptor?
                            ?: WrappedPropertyDescriptor()
                    )
                ProtoSymbolKind.FUNCTION_SYMBOL ->
                    symbolTable.referenceSimpleFunction(
                        descriptor as FunctionDescriptor?
                            ?: WrappedSimpleFunctionDescriptor()
                    )
                ProtoSymbolKind.TYPEALIAS_SYMBOL ->
                    symbolTable.referenceTypeAlias(
                        descriptor as TypeAliasDescriptor?
                            ?: WrappedTypeAliasDescriptor()
                    )
                ProtoSymbolKind.VARIABLE_SYMBOL ->
                    IrVariableSymbolImpl(
                        descriptor as VariableDescriptor?
                            ?: WrappedVariableDescriptor()
                    )
                ProtoSymbolKind.VALUE_PARAMETER_SYMBOL ->
                    IrValueParameterSymbolImpl(
                        descriptor as ParameterDescriptor?
                            ?: WrappedValueParameterDescriptor()
                    )
                ProtoSymbolKind.RECEIVER_PARAMETER_SYMBOL ->
                    IrValueParameterSymbolImpl(
                        descriptor as ParameterDescriptor? ?: WrappedReceiverParameterDescriptor()
                    )
                ProtoSymbolKind.PROPERTY_SYMBOL ->
                    symbolTable.referenceProperty(
                        descriptor as PropertyDescriptor? ?: WrappedPropertyDescriptor()
                    )
                ProtoSymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
                    IrLocalDelegatedPropertySymbolImpl(
                        descriptor as? VariableDescriptorWithAccessors ?: WrappedVariableDescriptorWithAccessor()
                    )
                else -> TODO("Unexpected classifier symbol kind: ${proto.kind}")
            }

            fun loadSymbolData(index: Int): ProtoSymbolData {
                return symbolProtosCache.getOrPut(index) {
                    loadSymbolProto(index)
                }
            }

            private fun loadTypeData(index: Int): ProtoType {
                return typeProtosCache.getOrPut(index) {
                    loadTypeProto(index)
                }
            }

            private fun loadString(index: Int): String {
                return stringsCache.getOrPut(index) {
                    loadStringProto(index)
                }
            }

            private fun isGlobalUniqID(uniqId: UniqId): Boolean {
                return uniqId in globalDeserializationState ||
                        descriptorReferenceDeserializer.checkIfSpecialDescriptorId(uniqId.index)
            }

            private fun getModuleForTopLevelId(key: UniqId): IrModuleDeserializer? {
                if (key in moduleReversedFileIndex) return this@IrModuleDeserializer
                return moduleDependencies.firstOrNull { key in it.moduleReversedFileIndex }
            }

            private fun getStateForID(key: UniqId): DeserializationState {
                if (key.isLocal) return fileLocalDeserializationState
                if (isGlobalUniqID(key)) return globalDeserializationState
                return getModuleForTopLevelId(key)?.moduleDeserializationState ?: handleNoModuleDeserializerFound(key)
            }

            private fun deserializeIrSymbolData(proto: ProtoSymbolData): IrSymbol {
                val key = proto.uniqId.uniqId()
                val topLevelKey = proto.topLevelUniqId.uniqId()

                val topLevelDeserializationState = getStateForID(topLevelKey)

                if (topLevelKey !in topLevelDeserializationState) {
                    topLevelDeserializationState.addUniqID(topLevelKey)
                }

                // If topLevel declaration is module-public and current declaration in not (e.g. value parameter)
                // they should be proccesed via different tables
                val deserializationState =
                    if (topLevelKey.isLocal xor key.isLocal) getStateForID(key) else topLevelDeserializationState

                val symbol = deserializationState.deserializedSymbols.getOrPut(key) {
                    val descriptor = if (proto.hasDescriptorReference()) {
                        deserializeDescriptorReference(proto.descriptorReference)
                    } else {
                        null
                    }

                    resolvedForwardDeclarations[key]?.let {
                        val fdState = getStateForID(it)
                        fdState.addUniqID(it)
                    }

                    referenceDeserializedSymbol(proto, descriptor)
                }
                if (symbol.descriptor is ClassDescriptor &&
                    symbol.descriptor !is WrappedDeclarationDescriptor<*> &&
                    symbol.descriptor.module.isForwardDeclarationModule
                ) {
                    forwardDeclarations.add(symbol)
                }

                return symbol
            }

            override fun deserializeDescriptorReference(proto: ProtoDescriptorReference) =
                descriptorReferenceDeserializer.deserializeDescriptorReference(
                    deserializeFqName(proto.packageFqName),
                    deserializeFqName(proto.classFqName),
                    deserializeString(proto.name),
                    if (proto.hasUniqId()) proto.uniqId.index else null,
                    isEnumEntry = proto.isEnumEntry,
                    isEnumSpecial = proto.isEnumSpecial,
                    isDefaultConstructor = proto.isDefaultConstructor,
                    isFakeOverride = proto.isFakeOverride,
                    isGetter = proto.isGetter,
                    isSetter = proto.isSetter,
                    isTypeParameter = proto.isTypeParameter
                )

            override fun deserializeIrSymbol(proto: ProtoSymbolIndex): IrSymbol {
                val symbolData = loadSymbolProto(proto.index)
                return deserializeIrSymbolData(symbolData)
            }

            override fun deserializeIrType(proto: ProtoTypeIndex): IrType {
                val typeData = loadTypeProto(proto.index)
                return deserializeIrTypeData(typeData)
            }

            override fun deserializeString(proto: ProtoStringIndex): String =
                loadStringProto(proto.index)

            override fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoopBase) =
                fileLoops.getOrPut(loopIndex, loopBuilder)

            override fun deserializeExpressionBody(proto: ProtoBodyIndex): IrExpression {
                if (deserializeBodies) {
                    val bodyData = loadExpressionBodyProto(proto.index)
                    return deserializeExpression(bodyData)
                } else {
                    val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
                    return IrErrorExpressionImpl(-1, -1, errorType, "Expression body is not deserialized yet")
                }
            }

            override fun deserializeStatementBody(proto: ProtoBodyIndex): IrElement {
                if (deserializeBodies) {
                    val bodyData = loadStatementBodyProto(proto.index)
                    return deserializeStatement(bodyData)
                } else {
                    val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
                    return IrBlockBodyImpl(-1, -1, listOf(IrErrorExpressionImpl(-1, -1, errorType, "Statement body is not deserialized yet")))
                }
            }

            fun deserializeFileAnnotationsIfFirstUse() {
                annotationsProto?.let {
                    file.annotations.addAll(deserializeAnnotations(it))
                    annotationsProto = null
                }
            }

            fun deserializeFileTopLevelDeclaration(key: UniqId) {

                fileLocalDeserializationState.addUniqID(key)

                fileLocalDeserializationState.processPendingDeclaration {
                    val declaration = deserializeDeclaration(it)
                    file.declarations.add(declaration)
                }
            }
        }

        private fun deserializeIrFile(fileProto: ProtoFile, fileIndex: Int): IrFile {

            val fileName = fileProto.fileEntry.name

            val fileEntry = NaiveSourceBasedFileEntryImpl(fileName, fileProto.fileEntry.lineStartOffsetsList.toIntArray())

            val fileDeserializer = IrDeserializerForFile(fileProto.annotations, fileIndex, !deserializationStrategy.needBodies)

            val fqName = fileDeserializer.deserializeFqName(fileProto.fqName)

            val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

            val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
            val file = IrFileImpl(fileEntry, symbol, fqName)

            fileDeserializer.file = file
            fileToDeserializerMap[file] = fileDeserializer

            fileProto.declarationIdList.forEach {
                val uniqId = it.uniqId()
                assert(uniqId.isPublic)
                moduleReversedFileIndex.getOrPut(uniqId) { fileDeserializer }
            val fileUniqIdIndex = fileProto.declarationIdList.map { UniqId(it) }

            fileUniqIdIndex.forEach {
                moduleReversedFileIndex.getOrPut(it) { fileDeserializer }
            }

            val forceLoadedIds = deserializationStrategy.run {
                when {
                    theWholeWorld -> fileProto.declarationIdList
                    explicitlyExported -> fileProto.explicitlyExportedToCompilerList.map {
                        fileDeserializer.loadSymbolData(it.index).topLevelUniqId
                    }
                    else -> emptyList()
            for (d in fileProto.explicitlyExportedToCompilerList) {
                fileDeserializer.run {
                    fileLocalDeserializationState.addUniqID(UniqId(loadSymbolData(d).topLevelUniqIdIndex))
                }
            }

            if (deserializationStrategy.theWholeWorld) {
                for (id in fileUniqIdIndex) {
                    assert(id.isPublic)
                    moduleDeserializationState.addUniqID(id)
                }
            } else if (deserializationStrategy.explicitlyExported) {
                modulesWithReachableTopLevels.add(this)
            }

            forceLoadedIds.forEach { moduleDeserializationState.addUniqID(it.uniqId().also { i -> assert(i.isPublic) }) }

            return file
        }

        private fun deserializeIrModuleHeader(): IrModuleFragment {
            val fileCount = readFileCount(moduleDescriptor)

            val files = mutableListOf<IrFile>()

            for (i in 0 until fileCount) {
                files.add(deserializeIrFile(ProtoFile.parseFrom(readFile(moduleDescriptor, i), newInstance()), i))
            }


            return IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
        }

        fun deserializeAllModuleReachableTopLevels() {

            moduleDeserializationState.processPendingDeclaration {
                val fileDeserializer = moduleReversedFileIndex[it]
                    ?: error("No file deserializer for key $it")

                fileDeserializer.deserializeFileTopLevelDeclaration(it)
                fileDeserializer.deserializeFileAnnotationsIfFirstUse()
            }
        }

        fun addModuleReachableTopLevel(topLevelKey: UniqId) {
            moduleDeserializationState.reachableTopLevels.add(topLevelKey)
            modulesWithReachableTopLevels.add(this)
        }
    }

    protected abstract val descriptorReferenceDeserializer: DescriptorReferenceDeserializer

    protected val indexAfterKnownBuiltins = loadKnownBuiltinSymbols()

    private fun loadKnownBuiltinSymbols(): Long {
        var currentIndex = firstKnownBuiltinsIndex
        val globalDeserializedSymbols = globalDeserializationState.deserializedSymbols
        builtIns.knownBuiltins.forEach {
            globalDeserializedSymbols[UniqId(currentIndex, isLocal = false)] = it
            assert(symbolTable.referenceSimpleFunction(it.descriptor) == it)
            currentIndex++
        }
        return currentIndex
    }

    private val ByteArray.codedInputStream: org.jetbrains.kotlin.protobuf.CodedInputStream
        get() {
            val codedInputStream = org.jetbrains.kotlin.protobuf.CodedInputStream.newInstance(this)
            codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
            return codedInputStream
        }

    protected abstract fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId): ByteArray
    protected abstract fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int): ByteArray
    protected abstract fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int): ByteArray
    protected abstract fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int): ByteArray
    protected abstract fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int): ByteArray
    protected abstract fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int): ByteArray
    protected abstract fun readFileCount(moduleDescriptor: ModuleDescriptor): Int

    protected abstract fun checkAccessibility(declarationDescriptor: DeclarationDescriptor): Boolean
    protected open fun handleNoModuleDeserializerFound(key: UniqId): DeserializationState {
        error("Deserializer for declaration $key is not found")
    }

    private fun deserializeAllReachableTopLevels() {
        do {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeAllModuleReachableTopLevels()
        } while (modulesWithReachableTopLevels.isNotEmpty())
    }

    private fun findDeserializedDeclarationForDescriptor(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val topLevelDescriptor = descriptor.findTopLevelDescriptor() as DeclarationDescriptorWithVisibility

        // This is Native specific. Try to eliminate.
        if (topLevelDescriptor.module.isForwardDeclarationModule) return null

        require(checkAccessibility(topLevelDescriptor)) {
            "Locally accessible declarations should not be accessed here $topLevelDescriptor"
        }


        if (topLevelDescriptor !is DeserializedClassDescriptor && topLevelDescriptor !is DeserializedCallableMemberDescriptor) {
            return null
        }

        val descriptorUniqId = topLevelDescriptor.getUniqId()
            ?: error("Could not get descriptor uniq id for $topLevelDescriptor")
        val topLevelKey = UniqId(descriptorUniqId, isLocal = false)

        val moduleOfOrigin = topLevelDescriptor.module

        val moduleDeserializer = deserializersForModules[moduleOfOrigin] ?: error("No module deserializer found for $moduleOfOrigin")

        moduleDeserializer.addModuleReachableTopLevel(topLevelKey)

        deserializeAllReachableTopLevels()
        return topLevelDescriptor
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {

        if (!symbol.isBound) {
            findDeserializedDeclarationForDescriptor(symbol.descriptor) ?: return null
        }

        assert(symbol.isBound) {
            "findDeserializedDeclaration: symbol ${symbol} is unbound, descriptor = ${symbol.descriptor}, hash = ${symbol.descriptor.hashCode()}"
        }

        return symbol.owner as IrDeclaration
    }

    // TODO: This is Native specific. Eliminate me.
    override fun declareForwardDeclarations() {
        if (forwardModuleDescriptor == null) return

        val packageFragments = forwardDeclarations.map { it.descriptor.findPackage() }.distinct()

        // We don't bother making a real IR module here, as we have no need in it any later.
        // All we need is just to declare forward declarations in the symbol table
        // In case you need a full fledged module, turn the forEach into a map and collect
        // produced files into an IrModuleFragment.

        packageFragments.forEach { packageFragment ->
            val symbol = IrFileSymbolImpl(packageFragment)
            val file = IrFileImpl(NaiveSourceBasedFileEntryImpl("forward declarations pseudo-file"), symbol)
            val symbols = forwardDeclarations
                .filter { !it.isBound }
                .filter { it.descriptor.findPackage() == packageFragment }
            val declarations = symbols.map {

                val classDescriptor = it.descriptor as ClassDescriptor
                val declaration = symbolTable.declareClass(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                    classDescriptor,
                    classDescriptor.modality
                ) { symbol: IrClassSymbol -> IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin, symbol) }
                    .also {
                        it.parent = file
                    }
                declaration

            }
            file.declarations.addAll(declarations)
        }
    }

    private fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED
    ): IrModuleFragment {
        val deserializerForModule = deserializersForModules.getOrPut(moduleDescriptor) {
            IrModuleDeserializer(moduleDescriptor, deserializationStrategy)
        }
        // The IrModule and its IrFiles have been created during module initialization.
        return deserializerForModule.module
    }

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor): IrModuleFragment? {
        // TODO: consider skip deserializing explicitly exported declarations for libraries.
        // Now it's not valid because of all dependencies that must be computed.
        val deserializationStrategy =
            if (exportedDependencies.contains(moduleDescriptor)) {
                DeserializationStrategy.ALL
            } else {
                DeserializationStrategy.EXPLICITLY_EXPORTED
            }
        return deserializeIrModuleHeader(moduleDescriptor, deserializationStrategy)
    }

    fun deserializeFullModule(moduleDescriptor: ModuleDescriptor): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, DeserializationStrategy.ALL)

    fun deserializeOnlyHeaderModule(moduleDescriptor: ModuleDescriptor): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, DeserializationStrategy.ONLY_DECLARATION_HEADERS)
}

enum class DeserializationStrategy(val needBodies: Boolean, val explicitlyExported: Boolean, val theWholeWorld: Boolean) {
    ONLY_REFERENCED(true, false, false),
    ALL(true, true, true),
    EXPLICITLY_EXPORTED(true, true, false),
    ONLY_DECLARATION_HEADERS(false, false, false)
}
