/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.linkage.issues.*
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FileLocalSymbolProvider
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.moduleVisibilityProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.ModuleVisibilityProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.canBeReferencedFromOtherFiles
import org.jetbrains.kotlin.ir.util.isClassSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.toIdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.pollFirst
import org.jetbrains.kotlin.utils.putToMultiMap

@RequiresOptIn("Internal API of the IR linker. Not supposed to be used outside the linker.")
annotation class IrLinkerInternalApi

@OptIn(IrLinkerInternalApi::class)
abstract class KotlinIrLinker(
    val symbolTable: SymbolTable,
    private val friendModules: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    val errorCallback: (String) -> Unit,
    val deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
) : IrProvider {
    constructor(
        configuration: CompilerConfiguration,
        symbolTable: SymbolTable,
        friendModules: Map<KotlinLibrary, Collection<KotlinLibrary>>,
        deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
    ) : this(
        symbolTable,
        friendModules,
        errorCallback = { configuration.report(PartialLinkageDiagnostics.IR_LINKER_ERROR, it) },
        deserializedSymbolPostProcessor
    )

    val anyClass = symbolTable.referenceClass(StandardClassIds.Any.toIdSignature())
    val unitClass = symbolTable.referenceClass(StandardClassIds.Unit.toIdSignature())
    val nothingClass = symbolTable.referenceClass(StandardClassIds.Nothing.toIdSignature())

    @IrLinkerInternalApi
    val irInterner = IrInterningService()

    @IrLinkerInternalApi
    val fileEntryDeserializer = FileEntryDeserializer(irInterner)

    protected val fileLocalSymbolProvider = object : FileLocalSymbolProvider {
        override fun referenceSimpleFunction(file: IrFile, signature: IdSignature.FileLocalSignature): IrSimpleFunctionSymbol =
            referenceCallable(file, signature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

        override fun referenceProperty(file: IrFile, signature: IdSignature.FileLocalSignature): IrPropertySymbol =
            referenceCallable(file, signature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

        private fun referenceCallable(
            file: IrFile,
            signature: IdSignature.FileLocalSignature,
            symbolKind: BinarySymbolData.SymbolKind,
        ): IrSymbol {
            val moduleDeserializer = firstNotNullOfModuleDeserializersOrNull(file.packageFqName) { moduleDeserializer ->
                moduleDeserializer.fileDeserializers[file]
            } ?: error("No file deserializer found for file: ${file.render()}")

            return moduleDeserializer.symbolDeserializer.deserializeSymbolWithOwnerInCurrentFile(signature, symbolKind)
        }
    }

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
    @IrLinkerInternalApi
    val modulesWithReachableTopLevels = linkedSetOf<IrModuleDeserializer>()

    protected val klibDeserializers = linkedMapOf<KotlinLibrary, IrModuleDeserializer>()
    private val nonKlibDeserializers = mutableListOf<IrModuleDeserializer>()
    private val moduleDeserializersByPackageName = mutableMapOf<FqName, MutableList<IrModuleDeserializer>>()
    private val moduleDeserializersWithUnknownPackageNames = mutableListOf<IrModuleDeserializer>()

    val allModuleDeserializers: List<IrModuleDeserializer>
        get() = klibDeserializers.values + nonKlibDeserializers

    val allModuleFragments: List<IrModuleFragment>
        get() = allModuleDeserializers.map { it.moduleFragment }

    abstract val irMangler: KotlinMangler.IrMangler

    abstract val fakeOverrideBuilder: IrLinkerFakeOverrideProvider

    private val triedToDeserializeDeclarationForSymbol = hashSetOf<IrSymbol>()

    abstract val partialLinkageSupport: PartialLinkageSupportForLinker

    open val moduleDependencyTracker: IrModuleDependencyTracker get() = IrModuleDependencyTracker.DISABLED

    // Represents the fact that the linkage process was finished.
    // In case if a symbol was asked after deserialization, we would like to make sure that it is correctly and fully linked.
    private var finished = false

    @IrLinkerInternalApi
    fun deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
        idSignature: IdSignature,
        symbolKind: BinarySymbolData.SymbolKind,
        moduleDeserializer: IrModuleDeserializer,
    ): IrSymbol {
        check(idSignature.canBeReferencedFromOtherFiles) {
            "An attempt to run a lookup in IR linker for a signature that is supposed to be limited to the single file: ${idSignature::class.java.simpleName}, ${idSignature.render()}"
        }

        val topLevelSignature: IdSignature = idSignature.topLevelSignature()

        // Note: The top-level symbol might be gone in newer version of dependency KLIB. Then the KLIB that was compiled against
        // the older version of dependency KLIB will still have a reference to non-existing symbol. And the linker will have to
        // handle such situation appropriately. See KT-41378.

        var currentModuleIfSearched: IrModuleDeserializer? = null
        var actualModuleDeserializer: IrModuleDeserializer? = null
        if (moduleDeserializer.preferLinkingToTheCurrentModule) {
            // First, look for the declaration in the current module (the module with a use-site of this signature).
            // This influences how duplicated declarations are handled. Longer explanation:
            //
            // At first, this was done as an optimization. Any given symbol is most likely to be found in the same Klib as it is used from.
            // So it made sense to check it first, only then iterate over all other Klibs.
            // Now, with an introduction of package index (KT-84837), this optimization becomes mostly unnecessary. We expect it's OK to
            // iterate over all Klibs to define a given package, as usually there should be only one or very few.
            // So this logic could be removed. However, we keep it to preserve the old behavior when two Klibs define the same declaration
            // (even though that behavior is very much imperfect) until we have a definite answer to this problem (KT-82172).
            // Hence, this logic could be theoretically removed. However, we keep it to preserve the old behavior when two Klibs define the
            // same declaration (even though the behavior is very much imperfect) until we have a definite answer to this problem (KT-82172).
            // It goes like this: Assuming there are two Klibs, libA and libB, both defining a class C, then:
            // - If both libA and libB reference class C, each will try to link to "their own" version of the class. In practice,
            //      this will usually fail, with the second deserialization call failing with an exception like "Class C is already bound".
            // - If only libA or libB reference class C, and that reference is the first one met by the linker, the corresponding version
            //     of class C will be linked.
            // - Otherwise, class C will be arbitrarily linked from either libA or libB.
            if (topLevelSignature in moduleDeserializer) {
                actualModuleDeserializer = moduleDeserializer
            }
            currentModuleIfSearched = moduleDeserializer
        }
        if (actualModuleDeserializer == null) {
            actualModuleDeserializer = firstModuleDeserializerOrNull(topLevelSignature.packageFqName()) {
                it != currentModuleIfSearched && topLevelSignature in it
            }
        }

        // Note: It might happen that the top-level symbol still exists in KLIB, but nested symbol has been removed.
        // Then the `actualModuleDeserializer` will be non-null, but `actualModuleDeserializer.tryDeserializeIrSymbol()` call
        // might return null (like KonanInteropModuleDeserializer does) or non-null unbound symbol (like JsModuleDeserializer does).
        val symbol: IrSymbol? = actualModuleDeserializer?.tryDeserializeIrSymbol(idSignature, symbolKind)

        return if (symbol != null) {
            moduleDependencyTracker.trackDependency(
                fromModule = moduleDeserializer.moduleFragment,
                toModule = actualModuleDeserializer.moduleFragment
            )

            symbol
        } else
            referenceDeserializedSymbol(symbolTable, null, symbolKind, idSignature)
    }

    private fun resolveModuleDeserializer(idSignature: IdSignature): IrModuleDeserializer? =
        firstModuleDeserializerOrNull(idSignature.packageFqName()) { idSignature in it }

    /**
     * Returns the first module deserializer that may define declarations in the package [packageFqName]
     * and matches the given [filter], or `null` if no such module deserializer is found.
     *
     * Similar to [firstOrNull] from stdlib, but only checks module deserializers that are relevant for [packageFqName],
     * in the following order:
     * 1. Module deserializers whose list of defined package names includes [packageFqName].
     * 2. Module deserializers that cannot tell which packages they define
     *    ([IrModuleDeserializer.contributedPackageNames] returns `null`). These are checked for every package.
     *
     * Inside each group, module deserializers are checked in the order they were registered with [createAndRegisterModuleDeserializer].
     */
    private inline fun firstModuleDeserializerOrNull(
        packageFqName: FqName,
        filter: (IrModuleDeserializer) -> Boolean,
    ): IrModuleDeserializer? = moduleDeserializersByPackageName[packageFqName]?.firstOrNull(filter)
        ?: moduleDeserializersWithUnknownPackageNames.firstOrNull(filter)

    /**
     * Applies the given [transformer] to module deserializers that may define declarations in the package [packageFqName]
     * and returns the first non-null result, or `null` if [transformer] returns `null` for all of them.
     *
     * Similar to [firstNotNullOfOrNull] from stdlib, but only checks module deserializers that are relevant for [packageFqName],
     * in the following order:
     * 1. Module deserializers whose list of defined package names includes [packageFqName].
     * 2. Module deserializers that cannot tell which packages they define
     *    ([IrModuleDeserializer.contributedPackageNames] returns `null`). These are checked for every package.
     *
     * Inside each group, module deserializers are checked in the order they were registered with [createAndRegisterModuleDeserializer].
     */
    private inline fun <T : Any> firstNotNullOfModuleDeserializersOrNull(
        packageFqName: FqName,
        transformer: (IrModuleDeserializer) -> T?,
    ): T? = moduleDeserializersByPackageName[packageFqName]?.firstNotNullOfOrNull(transformer)
        ?: moduleDeserializersWithUnknownPackageNames.firstNotNullOfOrNull(transformer)

    protected abstract fun createModuleDeserializer(
        moduleFragment: IrModuleFragment,
        klib: KotlinLibrary?,
        strategyResolver: DeserializationStrategyResolver,
    ): IrModuleDeserializer

    protected abstract fun isBuiltInModule(module: IrModuleFragment): Boolean

    fun getBuiltInsModule(): IrModuleFragment =
        allModuleDeserializers.firstOrNull { it is IrModuleDeserializerWithBuiltIns }?.moduleFragment
            ?: error("The module with the built-ins has not been deserialized yet")

    /**
     * Run deserialization of top-level declarations previously scheduled for deserialization in the current [KotlinIrLinker].
     */
    fun deserializeAllReachableTopLevels() {
        while (true) {
            val moduleDeserializer = modulesWithReachableTopLevels.pollFirst() ?: break
            moduleDeserializer.deserializeReachableDeclarations()
        }
    }

    /**
     * Deserializes declarations for all unbound symbols in [symbolTable] and everything reachable from them,
     * repeating until the set of unbound symbols stops changing.
     *
     * Must be called before [finalizeIrLinkage], which handles the symbols that remain unbound.
     */
    fun drainDeserializationQueue() {
        // Deserializing a reference may lead to new unbound references, so we loop until none are left.
        var unbound: Set<IrSymbol> = emptySet()
        do {
            val prevUnbound: Set<IrSymbol> = unbound
            unbound = symbolTable.descriptorExtension.allUnboundSymbols
            for (symbol: IrSymbol in unbound) {
                // Symbol could get bound as a side effect of deserializing other symbols.
                if (!symbol.isBound && triedToDeserializeDeclarationForSymbol.add(symbol)) {
                    val signature = symbol.signature ?: continue
                    getSymbolAndPutIntoQueue(signature, symbol.kind())
                }
            }
            deserializeAllReachableTopLevels()
            // We wait for the unbound to stabilize on fake overrides.
        } while (unbound != prevUnbound)
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

    protected open fun createTypeSystemContext(irBuiltIns: IrBuiltIns): IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? =
        deserializeOrResolveDeclaration(symbol)

    private fun deserializeOrResolveDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (!symbol.isBound) {
            try {
                if (!findDeserializedDeclarationForSymbol(symbol)) return null
            } catch (e: IrSymbolTypeMismatchException) {
                SymbolTypeMismatch(e).raiseIssue(errorCallback)
            }
        }

        if (!symbol.isBound) return null

        //assert(symbol.isBound) {
        //    "getDeclaration: symbol $symbol is unbound, descriptor = ${symbol.descriptor}, signature = ${symbol.signature}"
        //}

        return symbol.owner as IrDeclaration
    }

    /**
     * Retrieves the symbol associated with the given signature and kind.
     *
     * It is guaranteed that the returned symbol is not null if such a symbol belongs to the module represented by the current deserializer.
     * The returned symbol may be unbound (and put in the deserialization queue to be deserialized later).
     *
     * However, it is guaranteed that the returned symbol is always bound if [getSymbolAndPutIntoQueue] is called after the
     * main part of the deserialization process has been finished, i.e. after [finalizeIrLinkage] was called.
     *
     * @return The symbol for the given signature and kind if successfully found; otherwise, null.
     */
    fun getSymbolAndPutIntoQueue(signature: IdSignature, kind: BinarySymbolData.SymbolKind): IrSymbol? {
        // TODO (KT-91417): Need to make it without an excessive IrModuleDeserializer.contains() call!
        val moduleDeserializer = resolveModuleDeserializer(signature) ?: return null
        return moduleDeserializer.tryDeserializeIrSymbol(signature, kind).also {
            if (finished) deserializeAllReachableTopLevels()
        }
    }


    /**
     * Drops everything interned by [irInterner] during IR deserialization to free memory.
     *
     * Call it only when deserialization is over: instances interned afterwards are not deduplicated
     * against the ones interned before.
     */
    fun clearInternedData() {
        irInterner.reset()
    }

    /**
     * Completes IR linkage: builds fake overrides, lets the partial linkage replace the symbols that remain unbound
     * with stubs, and checks that no unbound symbols are left.
     *
     * Must be called after [drainDeserializationQueue]. Declarations requested later are linked right at deserialization.
     */
    fun finalizeIrLinkage(irBuiltIns: IrBuiltIns) {
        finished = true

        // We have to exclude classifiers with unbound symbols in supertypes and in type parameter upper bounds from F.O. generation
        // to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
        partialLinkageSupport.exploreClassifiers(fakeOverrideBuilder)
        partialLinkageSupport.preprocessBeforeFakeOverridesBuilding(symbolTable, fakeOverrideBuilder)

        // Fake override generator creates new IR declarations. This may have effect of binding for certain symbols.
        fakeOverrideBuilder.provideFakeOverrides(createTypeSystemContext(irBuiltIns))
        triedToDeserializeDeclarationForSymbol.clear()

        // Finally, generate stubs for the remaining unbound symbols and patch every usage of any unbound symbol inside the IR tree.
        partialLinkageSupport.generateStubsAndPatchUsages(irBuiltIns, symbolTable)
        allModuleDeserializers.forEach { if (it is IrModuleDeserializerWithBuiltIns) it.finish(irBuiltIns) }

        checkNoUnboundSymbols("at the end of IR linkage process")
    }

    fun createAndRegisterModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        strategyResolver: DeserializationStrategyResolver,
    ): IrModuleDeserializer {
        val moduleFragment = IrModuleFragmentImpl(moduleDescriptor)
        moduleFragment.kotlinLibrary = kotlinLibrary
        moduleFragment.friendLibraries = friendModules[kotlinLibrary]?.toSet().orEmpty()
        moduleFragment.moduleVisibilityProvider = IrLinkerModuleVisibilityProvider

        val deserializer = maybeWrapWithBuiltIn(
            createModuleDeserializer(
                moduleFragment = moduleFragment,
                klib = kotlinLibrary,
                strategyResolver = strategyResolver
            )
        )

        if (kotlinLibrary != null) {
            require(klibDeserializers[kotlinLibrary] == null) { "Deserializers for ${kotlinLibrary.path} is already registered" }
            klibDeserializers[kotlinLibrary] = deserializer
        } else {
            nonKlibDeserializers += deserializer
        }

        when (val contributedPackageNames: Set<FqName>? = deserializer.contributedPackageNames) {
            null -> moduleDeserializersWithUnknownPackageNames += deserializer
            else -> for (packageName in contributedPackageNames) {
                moduleDeserializersByPackageName.putToMultiMap(packageName, deserializer)
            }
        }

        moduleDependencyTracker.addModuleForTracking(module = deserializer.moduleFragment)

        return deserializer
    }

    private fun maybeWrapWithBuiltIn(
        moduleDeserializer: IrModuleDeserializer,
    ): IrModuleDeserializer =
        if (isBuiltInModule(moduleDeserializer.moduleFragment)) {
            IrModuleDeserializerWithBuiltIns(
                moduleDeserializer.moduleFragment,
                symbolTable,
                irMangler,
                { clazz, signature -> fakeOverrideBuilder.enqueueClass(clazz, signature, moduleDeserializer.compatibilityMode) },
                moduleDeserializer
            )
        } else moduleDeserializer

    /**
     * Finds signatures of all top-level callables (functions or properties) with the given [callableId]
     * across all modules known to the linker.
     *
     * The result may have more than one signature: there can be several overloaded functions with the same
     * [CallableId], and they can even be located in different modules.
     *
     * Note: This function does not return symbols and does not schedule deserialization of the found callables.
     * To get a symbol, pass the signature to the appropriate symbol lookup function of the linker.
     */
    fun getAllMatchingSignatures(callableId: CallableId, signatureKind: BinarySymbolData.SymbolKind): List<IdSignature> {
        val topLevelCallableId = TopLevelCallableIdAndKind(callableId, signatureKind)

        return buildList {
            fun List<IrModuleDeserializer>.collectMatchingSignatures() {
                forEach { it.collectAllMatchingSignatures(topLevelCallableId, output = this@buildList) }
            }

            moduleDeserializersByPackageName[callableId.packageName]?.collectMatchingSignatures()
            moduleDeserializersWithUnknownPackageNames.collectMatchingSignatures()
        }
    }
}

/**
 * A key for looking up a declaration (or its symbol) in the linker: the [signature] tells which declaration
 * it is, and the [kind] tells what kind of declaration (and so what kind of [IrSymbol]) is expected.
 *
 * Unlike [TopLevelCallableIdAndKind], which may correspond to several overloaded callables,
 * a [SignatureAndKind] denotes exactly one declaration.
 */
data class SignatureAndKind(val signature: IdSignature, val kind: BinarySymbolData.SymbolKind) {
    override fun toString() = "${signature.render()}, kind=$kind"
}

/**
 * A key for looking up top-level callables (functions or properties) in a module by their [CallableId].
 *
 * A [CallableId] alone does not identify a single declaration: there can be several overloaded functions
 * with the same [CallableId], and a function and a property can share the same [CallableId] too.
 * So [kind] is used to tell functions from properties, and a single [TopLevelCallableIdAndKind]
 * may still correspond to multiple [IdSignature]s (one per overload).
 *
 * @property callableId The package FQ name and the name of the top-level callable.
 * @property kind The kind of the callable. Only [BinarySymbolData.SymbolKind.FUNCTION_SYMBOL] and
 *   [BinarySymbolData.SymbolKind.PROPERTY_SYMBOL] are allowed.
 */
data class TopLevelCallableIdAndKind(val callableId: CallableId, val kind: BinarySymbolData.SymbolKind) {
    val packageFqName: FqName get() = callableId.packageName

    constructor(signature: IdSignature.CommonSignature, kind: BinarySymbolData.SymbolKind) : this(
        callableId = signature.checkSignature().toCallableId(),
        kind = kind,
    )

    constructor(symbol: IrSymbol) : this(
        callableId = symbol.getCallableSignatureOrFail().toCallableId(),
        kind = symbol.kind(),
    )

    init {
        require(callableId.className == null && callableId.classId == null) {
            "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for a class member: $kind, $callableId"
        }

        require(kind == BinarySymbolData.SymbolKind.FUNCTION_SYMBOL || kind == BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) {
            "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for unsupported symbol kind: $kind, $callableId"
        }
    }

    override fun toString() = "$callableId, kind=$kind"

    companion object {
        private fun IrSymbol.getCallableSignatureOrFail(): IdSignature.CommonSignature {
            when (val signature = signature) {
                is IdSignature.CommonSignature -> return signature.checkSignature()
                null -> error("Can not create ${TopLevelCallableIdAndKind::class.java.simpleName}, no signature in symbol $this")
                else -> error("Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for symbol $this, unexpected signature type: ${signature::class.java.simpleName}, $signature")
            }
        }

        private fun IdSignature.CommonSignature.checkSignature(): IdSignature.CommonSignature {
            check(!isClassSignature()) { "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for class signature $this" }
            check(nameSegments.size == 1) { "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for a non-top-level declaration signature $this" }
            return this
        }

        private fun IdSignature.CommonSignature.toCallableId(): CallableId =
            CallableId(packageFqName(), Name.identifier(declarationFqName))
    }
}

/**
 * Defines which declarations of a deserialized IR file are loaded, and how much of each declaration.
 *
 * A strategy is resolved per file by [DeserializationStrategyResolver]. Every strategy is also a resolver
 * on its own that returns itself for any file, so it can be passed where a resolver is expected to apply
 * the same strategy to the whole module.
 *
 * Regardless of the strategy, a top-level declaration referenced from any other already deserialized
 * declaration is always deserialized on demand. Also regardless of the strategy, once any top-level declaration
 * of a file is deserialized, the declarations that the serializer recorded as explicitly exported in that file
 * are deserialized as well. The strategy decides what is loaded *in addition* to that, and whether function bodies
 * are loaded.
 *
 * @property onDemand The file is not added to [IrModuleFragment.files], however it is reported among
 *   [IrModuleDeserializer.fileDeserializers]. Nothing from the file is deserialized until something refers to one of
 *   its declarations.
 *   For example, when Kotlin/Native builds a per-file cache for a single file of `stdlib`, all other files
 *   of `stdlib` are [onDemand]: they must not get into the module fragment (otherwise code would be generated
 *   for them as well), but a function from such a file that is called from the cached file still has to be
 *   resolvable.
 * @property needBodies Bodies of all deserialized functions are loaded. For example, the main module compiled
 *   to JavaScript needs every function body, because the backend generates code for all of them.
 * @property needOnlyInlineBodies Bodies of inline functions (and of local functions declared inside them) are loaded,
 *   even if [needBodies] is not set. For example, when user code calls `list.map { it * 2 }`, the backend has to
 *   inline the body of `kotlin.collections.map` into the call site, so it needs that body even if `stdlib` itself
 *   is not going to be compiled.
 * @property explicitlyExported Declarations that the serializer recorded as explicitly exported to the compiler
 *   are deserialized even if nothing in the file is referenced. These are the roots required by the platform rather
 *   than by Kotlin code: `@JsExport` declarations on JS, or functions annotated with `@ExportForCppRuntime` on Native
 *   that are called from the C++ runtime. Without this flag, they are deserialized only together with some other
 *   referenced declaration of the same file. So a `@JsExport fun greet()` in a file of a dependency that no Kotlin
 *   code uses anything from would be silently dropped from the resulting JS module.
 * @property theWholeWorld All top-level declarations of the file are deserialized, whether referenced or not.
 *   For example, the `dump-ir` command of the `klib` tool must show every declaration of the library, not only
 *   those reachable from somewhere.
 */
enum class DeserializationStrategy(
    val onDemand: Boolean = false,
    val needBodies: Boolean = false,
    val needOnlyInlineBodies: Boolean = needBodies,
    val explicitlyExported: Boolean = false,
    val theWholeWorld: Boolean = false,
) : DeserializationStrategyResolver {
    /**
     * The file is hidden from the module fragment. Nothing from it is deserialized until one of its declarations
     * is referenced. No function bodies are loaded.
     *
     * Used for files of a library that are not being compiled right now but may still be referenced,
     * e.g. all files except the cached ones when Kotlin/Native builds a per-file cache.
     */
    ON_DEMAND(onDemand = true),

    /**
     * Everything in the file is deserialized: all top-level declarations, explicitly exported ones,
     * and all function bodies.
     *
     * Used for the module that is actually being compiled into a binary, e.g. the main module in JS
     * whole-program compilation, an exported dependency of a Kotlin/Native framework, or when dumping
     * the whole content of a library.
     */
    ALL(needBodies = true, explicitlyExported = true, theWholeWorld = true),

    /**
     * All top-level declarations of the file are deserialized, whether referenced or not, but no function
     * bodies are loaded.
     *
     * Used when every declaration has to be visible in the linked IR although nothing will be generated
     * from this library, e.g. when a library is fully present in the compiler cache, but we still need to
     * walk every declaration of it.
     */
    ALL_DECLARATION_HEADERS(theWholeWorld = true),

    /**
     * Only declarations reachable from elsewhere, plus explicitly exported ones, are deserialized,
     * and all of them get their function bodies.
     *
     * Used for regular dependencies in whole-program compilation (Kotlin/Native, Kotlin/JS): if the
     * program calls only `println()` from `stdlib`, then only `println()` and whatever it transitively
     * refers to are loaded and compiled, together with the platform roots like `@JsExport` declarations.
     */
    EXPLICITLY_EXPORTED(needBodies = true, explicitlyExported = true),

    /**
     * Only declarations reachable from elsewhere are deserialized, without any function bodies. Explicitly
     * exported declarations of a file are deserialized only once something else in that file is deserialized.
     *
     * Used when the code of a library is already compiled and only its declarations need to be
     * known for linkage, e.g. a library that is fully present in the Kotlin/Native cache. This is also
     * the default strategy of [IrModuleDeserializer].
     */
    ONLY_REFERENCED_DECLARATION_HEADERS,

    /**
     * Only declarations reachable from elsewhere are deserialized, and only inline functions get their bodies.
     * Explicitly exported declarations of a file are deserialized only once something else in that file is deserialized.
     *
     * Used when a library is not going to be compiled, but its inline functions still need to be inlined
     * into the code that is, e.g. unmodified files during JS incremental compilation, or a Kotlin/Native
     * library that is cached as a header cache.
     */
    WITH_INLINE_BODIES(needOnlyInlineBodies = true);

    init {
        check(!onDemand || !explicitlyExported)
        check(needOnlyInlineBodies || !needBodies)
    }

    override fun invoke(filePath: String) = this
}

fun interface DeserializationStrategyResolver {
    operator fun invoke(filePath: String): DeserializationStrategy
}

/**
 * This is an auxiliary attribute that is used to store [KotlinLibrary] instance for deserialized [IrModuleFragment].
 *
 * It is stamped by [KotlinIrLinker] for every module fragment created during IR linkage.
 *
 * The attribute stays `null` for module fragments that the linker did not create: the fragment of the module being
 * compiled, fragments on backends that have no klibs at all (JVM), and the single fragment that the frontend creates
 * for all binary dependencies at once, see `DependencyListForCliModule.Builder`.
 */
var IrModuleFragment.kotlinLibrary: KotlinLibrary? by irAttribute(copyByDefault = true)

/**
 * The list of friend Klibs, as specified by -Xfriend-modules.
 *
 * It is present only when [IrModuleFragment.kotlinLibrary] is present, i.e. in module fragments deserialized with IR linker.
 * In practice, it's present at the 2-nd stage of compilation. Friend dependencies on the 1-st stage are tracked by frontend structures.
 */
internal var IrModuleFragment.friendLibraries: Set<KotlinLibrary>? by irAttribute(copyByDefault = true)

private object IrLinkerModuleVisibilityProvider : ModuleVisibilityProvider {
    override fun canSeeInternals(thisModule: IrModuleFragment, otherModule: IrModuleFragment): Boolean {
        if (otherModule.moduleVisibilityProvider !is IrLinkerModuleVisibilityProvider) return false
        if (thisModule == otherModule) return true

        val fromModuleKlib = thisModule.kotlinLibrary ?: return false
        val toModuleKlib = otherModule.kotlinLibrary ?: return false
        return fromModuleKlib == toModuleKlib || thisModule.friendLibraries?.contains(toModuleKlib) == true
    }
}
