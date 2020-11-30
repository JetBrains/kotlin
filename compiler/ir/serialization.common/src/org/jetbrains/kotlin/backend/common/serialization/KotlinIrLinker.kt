/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite.newInstance
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual as ProtoActual
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>
) : IrDeserializer, FileLocalAwareLinker {

    // Kotlin-MPP related data. Consider some refactoring
    private val expectUniqIdToActualUniqId = mutableMapOf<IdSignature, IdSignature>()
    private val topLevelActualUniqItToDeserializer = mutableMapOf<IdSignature, IrModuleDeserializer>()
    private val expectSymbols = mutableMapOf<IdSignature, IrSymbol>()
    private val actualSymbols = mutableMapOf<IdSignature, IrSymbol>()

    private val modulesWithReachableTopLevels = mutableSetOf<IrModuleDeserializer>()

    protected val deserializersForModules = mutableMapOf<ModuleDescriptor, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: FakeOverrideBuilder

    abstract val translationPluginContext: TranslationPluginContext?

    private val haveSeen = mutableSetOf<IrSymbol>()

    private lateinit var linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>

    abstract inner class BasicIrModuleDeserializer(moduleDescriptor: ModuleDescriptor, override val klib: IrLibrary, override val strategy: DeserializationStrategy, private val containsErrorCode: Boolean = false) :
        IrModuleDeserializer(moduleDescriptor) {

        private val fileToDeserializerMap = mutableMapOf<IrFile, IrDeserializerForFile>()

        private inner class ModuleDeserializationState {
            private val filesWithPendingTopLevels = mutableSetOf<IrDeserializerForFile>()

            fun enqueueFile(fileDeserializer: IrDeserializerForFile) {
                filesWithPendingTopLevels.add(fileDeserializer)
                enqueueModule()
            }

            fun addIdSignature(key: IdSignature) {
                val fileDeserializer = moduleReversedFileIndex[key] ?: error("No file found for key $key")
                fileDeserializer.fileLocalDeserializationState.addIdSignature(key)

                enqueueFile(fileDeserializer)
            }

            fun processPendingDeclarations() {
                while (filesWithPendingTopLevels.isNotEmpty()) {
                    val pendingDeserializer = filesWithPendingTopLevels.first()

                    pendingDeserializer.deserializeFileImplicitDataIfFirstUse()
                    pendingDeserializer.deserializeAllFileReachableTopLevel()

                    filesWithPendingTopLevels.remove(pendingDeserializer)
                }
            }
        }

        private val moduleDeserializationState = ModuleDeserializationState()
        private val moduleReversedFileIndex = mutableMapOf<IdSignature, IrDeserializerForFile>()
        override val moduleDependencies by lazy {
            moduleDescriptor.allDependencyModules.filter { it != moduleDescriptor }.map { resolveModuleDeserializer(it) }
        }

        override fun init(delegate: IrModuleDeserializer) {
            val fileCount = klib.fileCount()

            val files = ArrayList<IrFile>(fileCount)

            for (i in 0 until fileCount) {
                val fileStream = klib.file(i).codedInputStream
                files.add(deserializeIrFile(ProtoFile.parseFrom(fileStream, newInstance()), i, delegate, containsErrorCode))
            }

            moduleFragment.files.addAll(files)

            fileToDeserializerMap.values.forEach { it.deserializeExpectActualMapping() }
        }

        override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
            fileToDeserializerMap[file]?.referenceSimpleFunctionByLocalSignature(idSignature)
                ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

        override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
            fileToDeserializerMap[file]?.referencePropertyByLocalSignature(idSignature)
                ?: error("No deserializer for file $file in module ${moduleDescriptor.name}")

        // TODO: fix to topLevel checker
        override fun contains(idSig: IdSignature): Boolean = idSig in moduleReversedFileIndex

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            assert(idSig.isPublic)

            val topLevelSignature = idSig.topLevelSignature()
            val fileDeserializer = moduleReversedFileIndex[topLevelSignature]
                ?: error("No file for $topLevelSignature (@ $idSig) in module $moduleDescriptor")

            val fileDeserializationState = fileDeserializer.fileLocalDeserializationState

            fileDeserializationState.addIdSignature(topLevelSignature)
            moduleDeserializationState.enqueueFile(fileDeserializer)

            return fileDeserializationState.deserializedSymbols.getOrPut(idSig) {
//                val descriptor = resolveSpecialSignature(idSig)
                val symbol = referenceDeserializedSymbol(symbolKind, idSig)

                handleExpectActualMapping(idSig, symbol)
            }
        }

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns, emptyList())

        private fun deserializeIrFile(fileProto: ProtoFile, fileIndex: Int, moduleDeserializer: IrModuleDeserializer, allowErrorNodes: Boolean): IrFile {

            val fileName = fileProto.fileEntry.name

            val fileEntry = NaiveSourceBasedFileEntryImpl(fileName, fileProto.fileEntry.lineStartOffsetsList.toIntArray())

            val fileDeserializer =
                IrDeserializerForFile(
                    fileProto.annotationList,
                    fileProto.actualsList,
                    fileIndex,
                    !strategy.needBodies,
                    strategy.inlineBodies,
                    moduleDeserializer, allowErrorNodes
                ).apply {

                    // Explicitly exported declarations (e.g. top-level initializers) must be deserialized before all other declarations.
                    // Thus we schedule their deserialization in deserializer's constructor.
                    fileProto.explicitlyExportedToCompilerList.forEach {
                        val symbolData = parseSymbolData(it)
                        val sig = deserializeIdSignature(symbolData.signatureId)
                        assert(!sig.isPackageSignature())
                        fileLocalDeserializationState.addIdSignature(sig.topLevelSignature())
                    }
                }

            val fqName = FqName(fileDeserializer.deserializeFqName(fileProto.fqNameList))

            val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

            val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
            val file = IrFileImpl(fileEntry, symbol, fqName)

            fileDeserializer.file = file
            fileToDeserializerMap[file] = fileDeserializer

            val fileSignatureIndex = fileProto.declarationIdList.map { fileDeserializer.deserializeIdSignature(it) to it }

            fileSignatureIndex.forEach {
                moduleReversedFileIndex.getOrPut(it.first) { fileDeserializer }
            }

            fileDeserializer.reversedSignatureIndex = fileSignatureIndex.toMap()

            if (strategy.theWholeWorld) {
                for (id in fileSignatureIndex) {
                    moduleDeserializationState.addIdSignature(id.first)
                }
                moduleDeserializationState.enqueueFile(fileDeserializer)
            } else if (strategy.explicitlyExported) {
                moduleDeserializationState.enqueueFile(fileDeserializer)
            }

            return file
        }

        override fun deserializeReachableDeclarations() {
            moduleDeserializationState.processPendingDeclarations()
        }

        private fun enqueueModule() {
            modulesWithReachableTopLevels.add(this)
        }

        override fun addModuleReachableTopLevel(idSig: IdSignature) {
            moduleDeserializationState.addIdSignature(idSig)
        }
    }

    inner class IrDeserializerForFile(
        private var annotations: List<ProtoConstructorCall>?,
        private val actuals: List<ProtoActual>,
        private val fileIndex: Int,
        onlyHeaders: Boolean,
        inlineBodies: Boolean,
        private val moduleDeserializer: IrModuleDeserializer,
        allowErrorNodes: Boolean
    ) :
        IrFileDeserializer(
            logger,
            builtIns,
            symbolTable,
            !onlyHeaders,
            fakeOverrideBuilder,
            allowErrorNodes
        )
    {

        private var fileLoops = mutableMapOf<Int, IrLoop>()

        lateinit var file: IrFile

        private val irTypeCache = mutableMapOf<Int, IrType>()

        override val deserializeInlineFunctions: Boolean = inlineBodies

        override val platformFakeOverrideClassFilter = fakeOverrideBuilder.platformSpecificClassFilter

        var reversedSignatureIndex = emptyMap<IdSignature, Int>()

        inner class FileDeserializationState {
            private val reachableTopLevels = LinkedHashSet<IdSignature>()
            val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

            fun addIdSignature(key: IdSignature) {
                reachableTopLevels.add(key)
            }

            fun processPendingDeclarations() {
                while (reachableTopLevels.isNotEmpty()) {
                    val reachableKey = reachableTopLevels.first()

                    val existedSymbol = deserializedSymbols[reachableKey]
                    if (existedSymbol == null || !existedSymbol.isBound) {
                        val declaration = deserializeDeclaration(reachableKey)
                        file.declarations.add(declaration)
                    }

                    reachableTopLevels.remove(reachableKey)
                }
            }
        }

        val fileLocalDeserializationState = FileDeserializationState()

        fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
            return deserializeDeclaration(loadTopLevelDeclarationProto(idSig), file)
        }

        fun deserializeExpectActualMapping() {
            actuals.forEach {
                val expectSymbol = parseSymbolData(it.expectSymbol)
                val actualSymbol = parseSymbolData(it.actualSymbol)

                val expect = deserializeIdSignature(expectSymbol.signatureId)
                val actual = deserializeIdSignature(actualSymbol.signatureId)

                assert(expectUniqIdToActualUniqId[expect] == null) {
                    "Expect signature $expect is already actualized by ${expectUniqIdToActualUniqId[expect]}, while we try to record $actual"
                }
                expectUniqIdToActualUniqId[expect] = actual
                // Non-null only for topLevel declarations.
                getModuleForTopLevelId(actual)?.let { md -> topLevelActualUniqItToDeserializer[actual] = md }
            }
        }

        private fun resolveSignatureIndex(idSig: IdSignature): Int {
            return reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
        }

        private fun readDeclaration(index: Int): CodedInputStream =
            moduleDeserializer.klib.irDeclaration(index, fileIndex).codedInputStream

        private fun loadTopLevelDeclarationProto(idSig: IdSignature): ProtoDeclaration {
            val idSigIndex = resolveSignatureIndex(idSig)
            return ProtoDeclaration.parseFrom(readDeclaration(idSigIndex), newInstance())
        }

        private fun readType(index: Int): CodedInputStream =
            moduleDeserializer.klib.type(index, fileIndex).codedInputStream

        private fun loadTypeProto(index: Int): ProtoType {
            return ProtoType.parseFrom(readType(index), newInstance())
        }

        private fun readSignature(index: Int): CodedInputStream =
            moduleDeserializer.klib.signature(index, fileIndex).codedInputStream

        private fun loadSignatureProto(index: Int): ProtoIdSignature {
            return ProtoIdSignature.parseFrom(readSignature(index), newInstance())
        }

        private fun readBody(index: Int): CodedInputStream =
            moduleDeserializer.klib.body(index, fileIndex).codedInputStream

        private fun loadStatementBodyProto(index: Int): ProtoStatement {
            return ProtoStatement.parseFrom(readBody(index), newInstance())
        }

        private fun loadExpressionBodyProto(index: Int): ProtoExpression {
            return ProtoExpression.parseFrom(readBody(index), newInstance())
        }

        private fun loadStringProto(index: Int): String {
            return String(moduleDeserializer.klib.string(index, fileIndex))
        }

        private fun getModuleForTopLevelId(idSignature: IdSignature): IrModuleDeserializer? {
            if (idSignature in moduleDeserializer) return moduleDeserializer
            return moduleDeserializer.moduleDependencies.firstOrNull { idSignature in it }
        }

        private fun findModuleDeserializer(idSig: IdSignature): IrModuleDeserializer {
            assert(idSig.isPublic)

            val topLevelSig = idSig.topLevelSignature()
            if (topLevelSig in moduleDeserializer) return moduleDeserializer
            return moduleDeserializer.moduleDependencies.firstOrNull { topLevelSig in it } ?: handleNoModuleDeserializerFound(idSig)
        }

        private fun referenceIrSymbolData(symbol: IrSymbol, signature: IdSignature) {
            assert(signature.isLocal)
            fileLocalDeserializationState.deserializedSymbols.putIfAbsent(signature, symbol)
        }

        private fun deserializeIrLocalSymbolData(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            assert(idSig.isLocal)

            if (idSig.hasTopLevel) {
                fileLocalDeserializationState.addIdSignature(idSig.topLevelSignature())
            }

            return fileLocalDeserializationState.deserializedSymbols.getOrPut(idSig) {
                referenceDeserializedSymbol(symbolKind, idSig)
            }
        }

        fun referenceSimpleFunctionByLocalSignature(idSignature: IdSignature) : IrSimpleFunctionSymbol =
            deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

        fun referencePropertyByLocalSignature(idSignature: IdSignature): IrPropertySymbol =
            deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

        private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            if (idSignature.isLocal) return deserializeIrLocalSymbolData(idSignature, symbolKind)

            return findModuleDeserializer(idSignature).deserializeIrSymbol(idSignature, symbolKind).also {
                haveSeen.add(it)
            }
        }

        override fun deserializeIrSymbolToDeclare(code: Long): Pair<IrSymbol, IdSignature> {
            val symbolData = parseSymbolData(code)
            val signature = deserializeIdSignature(symbolData.signatureId)
            return Pair(deserializeIrSymbolData(signature, symbolData.kind), signature)
        }

        fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

        override fun deserializeIrSymbol(code: Long): IrSymbol {
            val symbolData = parseSymbolData(code)
            val signature = deserializeIdSignature(symbolData.signatureId)
            return deserializeIrSymbolData(signature, symbolData.kind)
        }

        override fun deserializeIrType(index: Int): IrType {
            return irTypeCache.getOrPut(index) {
                val typeData = loadTypeProto(index)
                deserializeIrTypeData(typeData)
            }
        }

        override fun deserializeIdSignature(index: Int): IdSignature {
            val sigData = loadSignatureProto(index)
            return deserializeSignatureData(sigData)
        }

        override fun deserializeString(index: Int): String =
            loadStringProto(index)

        override fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoop) =
            fileLoops.getOrPut(loopIndex, loopBuilder)

        override fun deserializeExpressionBody(index: Int): IrExpression {
            return if (deserializeBodies) {
                val bodyData = loadExpressionBodyProto(index)
                deserializeExpression(bodyData)
            } else {
                val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
                IrErrorExpressionImpl(-1, -1, errorType, "Expression body is not deserialized yet")
            }
        }

        override fun deserializeStatementBody(index: Int): IrElement {
            return if (deserializeBodies) {
                val bodyData = loadStatementBodyProto(index)
                deserializeStatement(bodyData)
            } else {
                val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
                irFactory.createBlockBody(
                    -1, -1, listOf(IrErrorExpressionImpl(-1, -1, errorType, "Statement body is not deserialized yet"))
                )
            }
        }

        override fun referenceIrSymbol(symbol: IrSymbol, signature: IdSignature) {
            referenceIrSymbolData(symbol, signature)
        }

        fun deserializeFileImplicitDataIfFirstUse() {
            annotations?.let {
                file.annotations += deserializeAnnotations(it)
                annotations = null
            }
        }

        fun deserializeAllFileReachableTopLevel() {
            fileLocalDeserializationState.processPendingDeclarations()
        }
    }

    private val ByteArray.codedInputStream: CodedInputStream
        get() {
            val codedInputStream = CodedInputStream.newInstance(this)
            codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
            return codedInputStream
        }

    protected open fun handleNoModuleDeserializerFound(idSignature: IdSignature): IrModuleDeserializer {
        error("Deserializer for declaration $idSignature is not found")
    }

    protected open fun resolveModuleDeserializer(moduleDescriptor: ModuleDescriptor): IrModuleDeserializer {
        return deserializersForModules[moduleDescriptor] ?: error("No module deserializer found for $moduleDescriptor")
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: IrLibrary?,
        strategy: DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract val functionalInterfaceFactory: IrAbstractFunctionFactory

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    // TODO: the following code worth some refactoring in the nearest future

    private fun handleExpectActualMapping(idSig: IdSignature, rawSymbol: IrSymbol): IrSymbol {
        val referencingSymbol = if (idSig in expectUniqIdToActualUniqId.keys) {
            assert(idSig.run { IdSignature.Flags.IS_EXPECT.test() })
            wrapInDelegatedSymbol(rawSymbol).also { expectSymbols[idSig] = it }
        } else rawSymbol

        if (idSig in expectUniqIdToActualUniqId.values) {
            actualSymbols[idSig] = rawSymbol
        }

        return referencingSymbol
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, idSig: IdSignature): IrSymbol = symbolTable.run {
        when (symbolKind) {
            BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> IrAnonymousInitializerSymbolImpl()
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClassFromLinker(idSig)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructorFromLinker(idSig)
            BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameterFromLinker(idSig)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntryFromLinker(idSig)
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceFieldFromLinker(idSig)
            BinarySymbolData.SymbolKind.FIELD_SYMBOL -> referenceFieldFromLinker(idSig)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunctionFromLinker(idSig)
            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAliasFromLinker(idSig)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referencePropertyFromLinker(idSig)
            BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> IrVariableSymbolImpl()
            BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
                IrLocalDelegatedPropertySymbolImpl()
            else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
        }
    }

    private fun deserializeAllReachableTopLevels() {
        while (modulesWithReachableTopLevels.isNotEmpty()) {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    private fun findDeserializedDeclarationForSymbol(symbol: IrSymbol): DeclarationDescriptor? {
        assert(symbol.isPublicApi || symbol.descriptor.module === currentModule || platformSpecificSymbol(symbol))

        if (haveSeen.contains(symbol)) {
            return null
        }
        haveSeen.add(symbol)

        val descriptor = symbol.descriptor

        val moduleDeserializer = resolveModuleDeserializer(descriptor.module)

//        moduleDeserializer.deserializeIrSymbol(signature, symbol.kind())
        moduleDeserializer.declareIrSymbol(symbol)

        deserializeAllReachableTopLevels()
        if (!symbol.isBound) return null
        return descriptor
    }

    protected open fun platformSpecificSymbol(symbol: IrSymbol): Boolean = false

    private fun tryResolveCustomDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (!symbol.hasDescriptor) return null

        val descriptor = symbol.descriptor

        if (descriptor is WrappedDeclarationDescriptor<*>) return null
        if (descriptor is CallableMemberDescriptor) {
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                // skip fake overrides
                return null
            }
        }

        return translationPluginContext?.let { ctx ->
            linkerExtensions.firstNotNullResult {
                it.resolveSymbol(symbol, ctx)
            }?.also {
                require(symbol.owner == it)
            }
        }
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {

        if (!symbol.isPublicApi) {
            val descriptor = symbol.descriptor
            if (descriptor is WrappedDeclarationDescriptor<*>) return null
            if (!platformSpecificSymbol(symbol)) {
                if (descriptor.module !== currentModule) return null
            }
        }

        if (!symbol.isBound) {
            findDeserializedDeclarationForSymbol(symbol) ?: tryResolveCustomDeclaration(symbol) ?: return null
        }

        // TODO: we do have serializations for those, but let's just create a stub for now.
        if (!symbol.isBound && (symbol.descriptor.isExpectMember || symbol.descriptor.containingDeclaration?.isExpectMember == true))
            return null

        if (!symbol.isBound) return null

        //assert(symbol.isBound) {
        //    "getDeclaration: symbol $symbol is unbound, descriptor = ${symbol.descriptor}, signature = ${symbol.signature}"
        //}

        return symbol.owner as IrDeclaration
    }

    override fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol? {
        if (idSignature.isPublic) return null
        return deserializersForModules[parent.file.packageFragmentDescriptor.containingDeclaration]?.referenceSimpleFunctionByLocalSignature(parent.file, idSignature)
            ?: error("No module deserializer for ${parent.render()}")
    }

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPublic) return null
        return deserializersForModules[parent.file.packageFragmentDescriptor.containingDeclaration]?.referencePropertyByLocalSignature(parent.file, idSignature)
            ?: error("No module deserializer for ${parent.render()}")
    }

    protected open fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer =
        CurrentModuleDeserializer(moduleFragment, dependencies)

    override fun init(moduleFragment: IrModuleFragment?, extensions: Collection<IrDeserializer.IrLinkerExtension>) {
        linkerExtensions = extensions
        if (moduleFragment != null) {
            val currentModuleDependencies = moduleFragment.descriptor.allDependencyModules.map {
                deserializersForModules[it] ?: error("No deserializer found for $it")
            }
            val currentModuleDeserializer = createCurrentModuleDeserializer(moduleFragment, currentModuleDependencies)
            deserializersForModules[moduleFragment.descriptor] =
                maybeWrapWithBuiltInAndInit(moduleFragment.descriptor, currentModuleDeserializer)
        }
        deserializersForModules.values.forEach { it.init() }
    }

    override fun postProcess() {
        finalizeExpectActualLinker()
        fakeOverrideBuilder.provideFakeOverrides()
        haveSeen.clear()

        // TODO: fix IrPluginContext to make it not produce additional external reference
        // symbolTable.noUnboundLeft("unbound after fake overrides:")
    }

    // The issue here is that an expect can not trigger its actual deserialization by reachability
    // because the expect can not see the actual higher in the module dependency dag.
    // So we force deserialization of actuals for all deserialized expect symbols here.
    private fun finalizeExpectActualLinker() {
        expectUniqIdToActualUniqId.filter { topLevelActualUniqItToDeserializer[it.value] != null }.forEach {
            val expectSymbol = expectSymbols[it.key]
            val actualSymbol = actualSymbols[it.value]
            if (expectSymbol != null && (actualSymbol == null || !actualSymbol.isBound)) {
                topLevelActualUniqItToDeserializer[it.value]!!.addModuleReachableTopLevel(it.value)
                deserializeAllReachableTopLevels()
            }
        }

        // Now after all actuals have been deserialized, retarget delegating symbols from expects to actuals.
        expectUniqIdToActualUniqId.forEach {
            val expectSymbol = expectSymbols[it.key]
            val actualSymbol = actualSymbols[it.value]
            if (expectSymbol != null && actualSymbol != null) {
                when (expectSymbol) {
                    is IrDelegatingClassSymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrClassSymbol
                    is IrDelegatingEnumEntrySymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrEnumEntrySymbol
                    is IrDelegatingSimpleFunctionSymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrSimpleFunctionSymbol
                    is IrDelegatingConstructorSymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrConstructorSymbol
                    is IrDelegatingPropertySymbolImpl ->
                        expectSymbol.delegate = actualSymbol as IrPropertySymbol
                    else -> error("Unexpected expect symbol kind during actualization: $expectSymbol")
                }
            }
        }
    }

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED
    ): IrModuleFragment {
        val deserializerForModule = deserializersForModules.getOrPut(moduleDescriptor) {
            maybeWrapWithBuiltInAndInit(moduleDescriptor, createModuleDeserializer(moduleDescriptor, kotlinLibrary, deserializationStrategy))
        }
        // The IrModule and its IrFiles have been created during module initialization.
        return deserializerForModule.moduleFragment
    }

    private fun maybeWrapWithBuiltInAndInit(
        moduleDescriptor: ModuleDescriptor,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer =
        if (isBuiltInModule(moduleDescriptor)) IrModuleDeserializerWithBuiltIns(builtIns, functionalInterfaceFactory, moduleDeserializer)
        else moduleDeserializer

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment {
        // TODO: consider skip deserializing explicitly exported declarations for libraries.
        // Now it's not valid because of all dependencies that must be computed.
        val deserializationStrategy =
            if (exportedDependencies.contains(moduleDescriptor)) {
                DeserializationStrategy.ALL
            } else {
                DeserializationStrategy.EXPLICITLY_EXPORTED
            }
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, deserializationStrategy)
    }

    fun deserializeFullModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.ALL)

    fun deserializeOnlyHeaderModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.ONLY_DECLARATION_HEADERS)

    fun deserializeHeadersWithInlineBodies(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, DeserializationStrategy.WITH_INLINE_BODIES)
}

enum class DeserializationStrategy(
    val needBodies: Boolean,
    val explicitlyExported: Boolean,
    val theWholeWorld: Boolean,
    val inlineBodies: Boolean
) {
    ONLY_REFERENCED(true, false, false, true),
    ALL(true, true, true, true),
    EXPLICITLY_EXPORTED(true, true, false, true),
    ONLY_DECLARATION_HEADERS(false, false, false, false),
    WITH_INLINE_BODIES(false, false, false, true)
}
