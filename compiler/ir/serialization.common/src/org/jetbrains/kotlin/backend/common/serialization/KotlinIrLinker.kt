/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite.newInstance
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
    private val deserializeFakeOverrides: Boolean
) : IrDeserializer {

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
    private val fakeOverrideClassQueue = mutableListOf<IrClass>()

    private lateinit var linkerExtensions: Collection<IrDeserializer.IrLinkerExtension>

    abstract inner class BasicIrModuleDeserializer(moduleDescriptor: ModuleDescriptor, override val klib: IrLibrary, override val strategy: DeserializationStrategy, private val containsErrorCode: Boolean = false) :
        IrModuleDeserializer(moduleDescriptor) {

        private val fileToDeserializerMap = mutableMapOf<IrFile, IrFileDeserializer>()

        private inner class ModuleDeserializationState {
            private val filesWithPendingTopLevels = mutableSetOf<IrFileDeserializer>()

            fun enqueueFile(fileDeserializer: IrFileDeserializer) {
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
        private val moduleReversedFileIndex = mutableMapOf<IdSignature, IrFileDeserializer>()
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
s
            moduleFragment.files.addAll(files)

            fileToDeserializerMap.values.forEach { it.deserializeExpectActualMapping() }
        }

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

            return fileDeserializer.symbolDeserializer.deserializeIrSymbol(idSig, symbolKind).also {
                haveSeen.add(it)
            }
        }

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns, emptyList())

        private fun deserializeIrFile(fileProto: ProtoFile, fileIndex: Int, moduleDeserializer: IrModuleDeserializer, allowErrorNodes: Boolean): IrFile {

            val fileReader = IrLibraryFile(moduleDeserializer.klib, fileIndex)
            val file = fileReader.createFile(moduleDescriptor, fileProto)

            val fileDeserializer =
                IrFileDeserializer(
                    logger,
                    builtIns,
                    symbolTable,
                    file,
                    fileReader,
                    fileProto.explicitlyExportedToCompilerList,
                    fileProto.declarationIdList,
                    strategy.needBodies,
                    deserializeFakeOverrides,
                    fakeOverrideClassQueue,
                    allowErrorNodes,
                    fileProto.annotationList,
                    fileProto.actualsList,
                    strategy.inlineBodies,
                    moduleDeserializer,
                    fakeOverrideBuilder,
                    expectUniqIdToActualUniqId,
                    expectSymbols,
                    actualSymbols,
                    topLevelActualUniqItToDeserializer,
                    ::handleNoModuleDeserializerFound,
                )

            fileToDeserializerMap[file] = fileDeserializer

            val topLevelDeclarations = fileDeserializer.reversedSignatureIndex.keys
            topLevelDeclarations.forEach {
                moduleReversedFileIndex.putIfAbsent(it, fileDeserializer) // TODO Why not simple put?
            }

            if (strategy.theWholeWorld) {
                fileDeserializer.enqueueAllDeclarations()
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
        
        while (fakeOverrideClassQueue.isNotEmpty()) {
            val klass = fakeOverrideClassQueue.removeLast()
            fakeOverrideBuilder.provideFakeOverrides(klass)
        }

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

internal val ByteArray.codedInputStream: CodedInputStream
    get() {
        val codedInputStream = CodedInputStream.newInstance(this)
        codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
        return codedInputStream
    }