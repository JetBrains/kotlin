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
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
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

    protected val deserializedSymbols = mutableMapOf<UniqIdKey, IrSymbol>()
    private val reachableTopLevels = mutableSetOf<UniqIdKey>()
    private val deserializedTopLevels = mutableSetOf<UniqIdKey>()

    //TODO: This is Native specific. Eliminate me.
    private val forwardDeclarations = mutableSetOf<IrSymbol>()
    val resolvedForwardDeclarations = mutableMapOf<UniqIdKey, UniqIdKey>()

    protected val deserializersForModules = mutableMapOf<ModuleDescriptor, IrModuleDeserializer>()

    inner class IrModuleDeserializer(
        private val moduleDescriptor: ModuleDescriptor,
        private val deserializationStrategy: DeserializationStrategy
    ) {

        val fileToDeserializerMap = mutableMapOf<IrFile, IrDeserializerForFile>()
        // This is a heavy initializer
        val module = deserializeIrModuleHeader()
        private var moduleLoops = mutableMapOf<Int, IrLoopBase>()

        inner class IrDeserializerForFile(private var annotationsProto: ProtoAnnotations?, private val fileIndex: Int, onlyHeaders: Boolean) :
            IrFileDeserializer(logger, builtIns, symbolTable) {

            private val symbolProtosCache = mutableMapOf<Int, ProtoSymbolData>()
            private val typeProtosCache = mutableMapOf<Int, ProtoType>()
            private val stringsCache = mutableMapOf<Int, String>()

            private val declarationsBytes: ByteArray by lazy { TODO("") }
            private val bodiesBytes: ByteArray by lazy { TODO("") }
            private val symbolsBytes: ByteArray by lazy { TODO("") }
            private val typesBytes: ByteArray by lazy { TODO("") }
            private val stringsBytes: ByteArray by lazy { TODO("") }
            lateinit var file: IrFile

            private val deserializeBodies: Boolean = !onlyHeaders

            fun deserializeDeclaration(key: UniqIdKey): IrDeclaration {
                return deserializeDeclaration(loadTopLevelDeclarationProto(key), file)
            }

            private fun loadTopLevelDeclarationProto(uniqIdKey: UniqIdKey): ProtoDeclaration {
                val stream = reader(moduleDescriptor, fileIndex, uniqIdKey.uniqId).codedInputStream
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

            private fun deserializeIrSymbolData(proto: ProtoSymbolData): IrSymbol {
                val key = proto.uniqId.uniqIdKey(moduleDescriptor)
                val topLevelKey = proto.topLevelUniqId.uniqIdKey(moduleDescriptor)

                if (!deserializedTopLevels.contains(topLevelKey)) reachableTopLevels.add(topLevelKey)

                val symbol = deserializedSymbols.getOrPut(key) {
                    val descriptor = if (proto.hasDescriptorReference()) {
                        deserializeDescriptorReference(proto.descriptorReference)
                    } else {
                        null
                    }

                    resolvedForwardDeclarations[key]?.let {
                        if (!deserializedTopLevels.contains(it)) reachableTopLevels.add(it) // Assuming forward declarations are always top levels.
                    }

                    descriptor?.module?.let {
                        if (!deserializersForModules.containsKey(it) && !it.isForwardDeclarationModule) {
                            deserializeIrModuleHeader(it)!!
                        }
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
                moduleLoops.getOrPut(loopIndex, loopBuilder)

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

            // TODO: this is JS specific. Eliminate me.
            override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
                this@KotlinIrLinker.getPrimitiveTypeOrNull(symbol, hasQuestionMark)

            fun deserializeFileAnnotationsIfFirstUse() {
                annotationsProto?.let {
                    file.annotations.addAll(deserializeAnnotations(it))
                    annotationsProto = null
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

            fileProto.declarationIdList.forEach {
                val uniqIdKey = it.uniqIdKey(moduleDescriptor)
                reversedFileIndex.getOrPut(uniqIdKey) { mutableListOf() }.add(file)
                fileToDeserializerMap[file] = fileDeserializer
            }

            when (deserializationStrategy) {
                DeserializationStrategy.EXPLICITLY_EXPORTED -> {
                    fileProto.explicitlyExportedToCompilerList.forEach {
                        val symbolProto = fileDeserializer.loadSymbolData(it.index)
                        reachableTopLevels.add(symbolProto.topLevelUniqId.uniqIdKey(moduleDescriptor))
                    }
                }
                DeserializationStrategy.ALL -> {
                    fileProto.declarationIdList.forEach {
                        val uniqIdKey = it.uniqIdKey(moduleDescriptor)
                        reachableTopLevels.add(uniqIdKey)
                    }
                }
                else -> error("Unixpected deserialization strategy")
            }

            return file
        }

        private fun deserializeIrModuleHeader(): IrModuleFragment {
            val fileCount = readFileCount(moduleDescriptor)

            val files = mutableListOf<IrFile>()

            for (i in 0 until fileCount) {
                files.add(deserializeIrFile(ProtoFile.parseFrom(readFile(moduleDescriptor, i), newInstance()), i))
            }

//            val files = readFiles(moduleDescriptor).map {
//                deserializeIrFile(ProtoFile.parseFrom(it.codedInputStream, newInstance()))
//            }

            return IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
        }
    }

    // TODO: this is JS specific. Eliminate me.
    protected open fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean): IrSimpleType? = null

    protected abstract val descriptorReferenceDeserializer: DescriptorReferenceDeserializer

    protected val indexAfterKnownBuiltins = loadKnownBuiltinSymbols()

    private fun loadKnownBuiltinSymbols(): Long {
        var currentIndex = firstKnownBuiltinsIndex
        builtIns.knownBuiltins.forEach {
            deserializedSymbols[UniqIdKey(null, UniqId(currentIndex, isLocal = false))] = it
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

    private val reversedFileIndex = mutableMapOf<UniqIdKey, MutableList<IrFile>>()

    private val UniqIdKey.moduleOfOrigin
        get() =
            this.moduleDescriptor ?: reversedFileIndex[this]?.handleClashes(this)?.packageFragmentDescriptor?.containingDeclaration

    protected abstract fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId): ByteArray
    protected abstract fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int): ByteArray
    protected abstract fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int): ByteArray
    protected abstract fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int): ByteArray
    protected abstract fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int): ByteArray
    protected abstract fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int): ByteArray
    protected abstract fun readFileCount(moduleDescriptor: ModuleDescriptor): Int

    protected open fun List<IrFile>.handleClashes(uniqIdKey: UniqIdKey): IrFile {
        if (size == 1)
            return this[0]
        assert(size != 0)
        error("UniqId clash: ${uniqIdKey.uniqId.index}. Found in the " +
                      "[${this.joinToString { it.packageFragmentDescriptor.containingDeclaration.name.asString() }}]")
    }

    private fun deserializeAllReachableTopLevels() {
        do {
            val key = reachableTopLevels.first()
            val moduleOfOrigin = key.moduleOfOrigin

            if (deserializedSymbols[key]?.isBound == true ||
                // The key.moduleOrigin is null for uniqIds that we haven't seen in any of the library headers.
                // Just skip it for now and handle it elsewhere.
                moduleOfOrigin == null
            ) {

                reachableTopLevels.remove(key)
                deserializedTopLevels.add(key)
                continue
            }

            val moduleDeserializer = deserializersForModules[moduleOfOrigin] ?: error("No module found")
            val file = reversedFileIndex[key]!!.handleClashes(key)
            val fileDeserializer: IrModuleDeserializer.IrDeserializerForFile = moduleDeserializer.fileToDeserializerMap[file] ?: error("dkjfkljfls")
            val reachable = fileDeserializer.deserializeDeclaration(key)
            file.declarations.add(reachable)
            fileDeserializer.deserializeFileAnnotationsIfFirstUse()

            reachableTopLevels.remove(key)
            deserializedTopLevels.add(key)
        } while (reachableTopLevels.isNotEmpty())
    }

    private fun findDeserializedDeclarationForDescriptor(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val topLevelDescriptor = descriptor.findTopLevelDescriptor()

        // This is Native specific. Try to eliminate.
        if (topLevelDescriptor.module.isForwardDeclarationModule) return null

        if (topLevelDescriptor !is DeserializedClassDescriptor && topLevelDescriptor !is DeserializedCallableMemberDescriptor) {
            return null
        }

        val descriptorUniqId = topLevelDescriptor.getUniqId()
            ?: error("could not get descriptor uniq id for $topLevelDescriptor")
        val uniqId = UniqId(descriptorUniqId, isLocal = false)
        val topLevelKey = UniqIdKey(topLevelDescriptor.module, uniqId)

        // This top level descriptor doesn't have a serialized IR declaration.
        if (topLevelKey.moduleOfOrigin == null) return null

        reachableTopLevels.add(topLevelKey)

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
}

enum class DeserializationStrategy(val needBodies: Boolean) {
    ONLY_REFERENCED(true),
    ALL(true),
    EXPLICITLY_EXPORTED(true),
    ONLY_DECLARATION_HEADERS(false)
}
