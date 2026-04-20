/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.backend.common.linkage.issues.IrSymbolTypeMismatchException
import org.jetbrains.kotlin.backend.common.linkage.issues.SignatureIdNotFoundInModuleWithDependencies
import org.jetbrains.kotlin.backend.common.linkage.issues.SymbolTypeMismatch
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

abstract class KotlinIrLinker(
    private val currentModule: ModuleDescriptor?,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val exportedDependencies: List<ModuleDescriptor>,
    val errorCallback: (String) -> Unit,
    val deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
) : IrDeserializer, FileLocalAwareLinker {
    constructor(
        currentModule: ModuleDescriptor?,
        configuration: CompilerConfiguration,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        exportedDependencies: List<ModuleDescriptor>,
        deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
    ) : this(
        currentModule,
        builtIns,
        symbolTable,
        exportedDependencies,
        errorCallback = { configuration.report(PartialLinkageDiagnostics.IR_LINKER_ERROR, it) },
        deserializedSymbolPostProcessor
    )

    val irInterner = IrInterningService()
    val fileEntryDeserializer = FileEntryDeserializer(irInterner)

    /**
     * This is the queue of modules containing top-level declarations to be deserialized. This is
     * the third-layer queue on top of [BasicIrModuleDeserializer.ModuleDeserializationState.filesWithPendingTopLevels] and
     * [FileDeserializationState.reachableTopLevels].
     *
     * A module can be enqueued using [BasicIrModuleDeserializer.ModuleDeserializationState.enqueueFile].
     * TODO: provide a more clear API for enqueueing IR modules, KT-73819
     *
     * The deserialization happens on invocation of [deserializeAllReachableTopLevels]. This in its turn
     * invokes [IrModuleDeserializer.deserializeReachableDeclarations] for each scheduled module.
     *
     * Note: A module is removed from the queue after all top-level declarations scheduled for
     * deserialization in that module have been actually deserialized. Later the module can be enqueued
     * once again to deserialize other top-level declaration(s). This process can be repeated multiple times.
     */
    val modulesWithReachableTopLevels = linkedSetOf<IrModuleDeserializer>()

    protected val deserializersForModules = linkedMapOf<String, IrModuleDeserializer>()

    abstract val fakeOverrideBuilder: IrLinkerFakeOverrideProvider

    private val triedToDeserializeDeclarationForSymbol = hashSetOf<IrSymbol>()

    open val partialLinkageSupport: PartialLinkageSupportForLinker get() = PartialLinkageSupportForLinker.DISABLED

    open val returnUnboundSymbolsIfSignatureNotFound: Boolean
        get() = partialLinkageSupport.isEnabled

    open val moduleDependencyTracker: IrModuleDependencyTracker get() = IrModuleDependencyTracker.DISABLED

    fun deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
        idSignature: IdSignature,
        symbolKind: BinarySymbolData.SymbolKind,
        moduleDeserializer: IrModuleDeserializer
    ): IrSymbol {
        val topLevelSignature: IdSignature = idSignature.topLevelSignature()

        // Note: The top-level symbol might be gone in newer version of dependency KLIB. Then the KLIB that was compiled against
        // the older version of dependency KLIB will still have a reference to non-existing symbol. And the linker will have to
        // handle such situation appropriately. See KT-41378.
        //
        // TODO (KT-84837): The lookup for a IrModuleDeserializer should work through an index.
        val actualModuleDeserializer: IrModuleDeserializer? = if (topLevelSignature in moduleDeserializer) {
            moduleDeserializer
        } else {
            deserializersForModules.values.firstOrNull { topLevelSignature in it }
        }

        // Note: It might happen that the top-level symbol still exists in KLIB, but nested symbol has been removed.
        // Then the `actualModuleDeserializer` will be non-null, but `actualModuleDeserializer.tryDeserializeIrSymbol()` call
        // might return null (like KonanInteropModuleDeserializer does) or non-null unbound symbol (like JsModuleDeserializer does).
        val symbol: IrSymbol? = actualModuleDeserializer?.tryDeserializeIrSymbol(idSignature, symbolKind)

        if (symbol != null) {
            moduleDependencyTracker.trackDependency(
                fromModule = moduleDeserializer.moduleFragment,
                toModule = actualModuleDeserializer.moduleFragment
            )

            return symbol
        } else if (returnUnboundSymbolsIfSignatureNotFound)
            return referenceDeserializedSymbol(symbolTable, null, symbolKind, idSignature)
        else
            SignatureIdNotFoundInModuleWithDependencies(
                idSignature = idSignature,
                problemModuleDeserializer = moduleDeserializer,
            ).raiseIssue(errorCallback)
    }

    private fun resolveModuleDeserializer(irFile: IrFile): IrModuleDeserializer? {
        return deserializersForModules.values.firstOrNull { moduleDeserializer ->
            moduleDeserializer.fileDeserializers().any { it.file == irFile }
        }
    }

    private fun resolveModuleDeserializer(idSignature: IdSignature): IrModuleDeserializer? {
        return deserializersForModules.values.firstOrNull() { idSignature in it }
    }

    protected abstract fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer

    protected abstract fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean

    /**
     * Run deserialization of top-level declarations previously scheduled for deserialization in the current [KotlinIrLinker].
     */
    fun deserializeAllReachableTopLevels() {
        while (modulesWithReachableTopLevels.isNotEmpty()) {
            val moduleDeserializer = modulesWithReachableTopLevels.first()
            modulesWithReachableTopLevels.remove(moduleDeserializer)

            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    private fun findDeserializedDeclarationForSymbol(symbol: IrSymbol): Boolean {
        if (!triedToDeserializeDeclarationForSymbol.add(symbol)) return false

        val signature = symbol.signature ?: return false
        val moduleDeserializer = resolveModuleDeserializer(signature)
            ?: return false
        moduleDeserializer.declareIrSymbol(symbol)

        deserializeAllReachableTopLevels()

        return symbol.isBound
    }

    protected open fun platformSpecificSymbol(symbol: IrSymbol): Boolean = false

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? =
        deserializeOrResolveDeclaration(symbol)

    private fun deserializeOrResolveDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (!symbol.isPublicApi && symbol.hasDescriptor && !platformSpecificSymbol(symbol) &&
            symbol.descriptor.module !== currentModule
        ) return null

        if (!symbol.isBound) {
            try {
                if (!findDeserializedDeclarationForSymbol(symbol)) return null
            } catch (e: IrSymbolTypeMismatchException) {
                SymbolTypeMismatch(e).raiseIssue(errorCallback)
            }
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

    override fun tryReferencingSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature): IrSimpleFunctionSymbol? {
        if (idSignature.isPubliclyVisible) return null
        return resolveModuleDeserializer(file)?.referenceSimpleFunctionByLocalSignature(file, idSignature)
    }

    override fun tryReferencingPropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPubliclyVisible) return null
        return resolveModuleDeserializer(file)?.referencePropertyByLocalSignature(file, idSignature)
    }

    protected open fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment): IrModuleDeserializer =
        CurrentModuleDeserializer(moduleFragment)

    override fun init(moduleFragment: IrModuleFragment?) {
        if (moduleFragment != null) {
            val currentModuleDeserializer = createCurrentModuleDeserializer(moduleFragment)
            deserializersForModules[moduleFragment.name.asString()] =
                maybeWrapWithBuiltInAndInit(moduleFragment.descriptor, currentModuleDeserializer)
        }
        deserializersForModules.values.forEach { it.init() }
    }

    fun clear() {
        irInterner.reset()
    }

    override fun postProcess(inOrAfterLinkageStep: Boolean) {
        if (inOrAfterLinkageStep) {
            // We have to exclude classifiers with unbound symbols in supertypes and in type parameter upper bounds from F.O. generation
            // to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
            partialLinkageSupport.exploreClassifiers(fakeOverrideBuilder)
            partialLinkageSupport.preprocessBeforeFakeOverridesBuilding(symbolTable, fakeOverrideBuilder)
        }

        // Fake override generator creates new IR declarations. This may have effect of binding for certain symbols.
        fakeOverrideBuilder.provideFakeOverrides()
        triedToDeserializeDeclarationForSymbol.clear()

        if (inOrAfterLinkageStep) {
            // Finally, generate stubs for the remaining unbound symbols and patch every usage of any unbound symbol inside the IR tree.
            partialLinkageSupport.generateStubsAndPatchUsages(symbolTable)
            deserializersForModules.values.forEach { if (it is IrModuleDeserializerWithBuiltIns) it.finish(builtIns) }
        }
        // TODO: fix IrPluginContext to make it not produce additional external reference
        // symbolTable.noUnboundLeft("unbound after fake overrides:")
    }

    private fun topLevelKindToSymbolKind(kind: IrDeserializer.TopLevelSymbolKind): BinarySymbolData.SymbolKind {
        return when (kind) {
            IrDeserializer.TopLevelSymbolKind.CLASS_SYMBOL -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
            IrDeserializer.TopLevelSymbolKind.PROPERTY_SYMBOL -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
            IrDeserializer.TopLevelSymbolKind.FUNCTION_SYMBOL -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
            IrDeserializer.TopLevelSymbolKind.TYPEALIAS_SYMBOL -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        }
    }

    override fun resolveBySignatureInModule(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleName: Name): IrSymbol {
        val moduleDeserializer =
            deserializersForModules.entries.find { it.key == moduleName.asString() }?.value
                ?: error("No module for name '$moduleName' found")
        assert(signature == signature.topLevelSignature()) { "Signature '$signature' has to be top level" }
        if (signature !in moduleDeserializer) error("No signature $signature in module $moduleName")
        return moduleDeserializer.deserializeIrSymbolOrFail(signature, topLevelKindToSymbolKind(kind)).also {
            deserializeAllReachableTopLevels()
        }
    }

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        deserializationStrategy: (String) -> DeserializationStrategy = { DeserializationStrategy.ONLY_REFERENCED },
        _moduleName: String? = null
    ): IrModuleFragment {
        assert(kotlinLibrary != null || _moduleName != null) { "Either library or explicit name have to be provided $moduleDescriptor" }
        val moduleName = kotlinLibrary?.uniqueName?.let { "<$it>" } ?: _moduleName!!
        assert(moduleDescriptor.name.asString() == moduleName) {
            "${moduleDescriptor.name.asString()} != $moduleName"
        }

        val moduleFragment = deserializersForModules.getOrPut(moduleName) {
            maybeWrapWithBuiltInAndInit(
                moduleDescriptor = moduleDescriptor,
                moduleDeserializer = createModuleDeserializer(
                    moduleDescriptor = moduleDescriptor,
                    klib = kotlinLibrary,
                    strategyResolver = deserializationStrategy
                )
            )
        }.moduleFragment

        moduleFragment.kotlinLibrary = kotlinLibrary
        moduleDependencyTracker.addModuleForTracking(module = moduleFragment)

        // The IrModule and its IrFiles have been created during module initialization.
        return moduleFragment
    }

    protected fun maybeWrapWithBuiltInAndInit(
        moduleDescriptor: ModuleDescriptor,
        moduleDeserializer: IrModuleDeserializer
    ): IrModuleDeserializer =
        if (isBuiltInModule(moduleDescriptor)) {
            IrModuleDeserializerWithBuiltIns(
                builtIns,
                symbolTable,
                fakeOverrideBuilder.mangler,
                { clazz, signature -> fakeOverrideBuilder.enqueueClass(clazz, signature, moduleDeserializer.compatibilityMode) },
                moduleDeserializer
            )
        } else moduleDeserializer

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?, moduleName: String): IrModuleFragment {
        // TODO: consider skip deserializing explicitly exported declarations for libraries.
        // Now it's not valid because of all dependencies that must be computed.
        val deserializationStrategy: (String) -> DeserializationStrategy =
            if (exportedDependencies.contains(moduleDescriptor)) {
                { DeserializationStrategy.ALL }
            } else {
                { DeserializationStrategy.EXPLICITLY_EXPORTED }
            }
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, deserializationStrategy, moduleName)
    }

    fun deserializeFullModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.ALL })

    fun deserializeOnlyHeaderModule(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary?): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.ONLY_DECLARATION_HEADERS })

    fun deserializeHeadersWithInlineBodies(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary): IrModuleFragment =
        deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, { DeserializationStrategy.WITH_INLINE_BODIES })

    fun deserializeDirtyFiles(moduleDescriptor: ModuleDescriptor, kotlinLibrary: KotlinLibrary, dirtyFiles: Collection<String>): IrModuleFragment {
        return deserializeIrModuleHeader(moduleDescriptor, kotlinLibrary, {
            if (it in dirtyFiles) DeserializationStrategy.ALL
            else DeserializationStrategy.WITH_INLINE_BODIES
        })
    }
}

enum class DeserializationStrategy(
    val onDemand: Boolean,
    val needBodies: Boolean,
    val explicitlyExported: Boolean,
    val theWholeWorld: Boolean,
    val inlineBodies: Boolean
) {
    ON_DEMAND(true, false, false, false, false),
    ONLY_REFERENCED(false, true, false, false, true),
    ALL(false, true, true, true, true),
    EXPLICITLY_EXPORTED(false, true, true, false, true),
    ONLY_DECLARATION_HEADERS(false, false, false, false, false),
    WITH_INLINE_BODIES(false, false, false, false, true)
}

/** This is an auxiliary attribute that is used to store [KotlinLibrary] instance for deserialized [IrModuleFragment]. */
var IrModuleFragment.kotlinLibrary: KotlinLibrary? by irAttribute(copyByDefault = false)
    private set
