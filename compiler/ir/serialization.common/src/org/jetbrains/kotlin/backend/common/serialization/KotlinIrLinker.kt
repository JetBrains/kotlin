/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
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
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.toIdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.putToMultiMap

abstract class KotlinIrLinker(
    val symbolTable: SymbolTable,
    val errorCallback: (String) -> Unit,
    val deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
) : IrDeserializer {
    constructor(
        configuration: CompilerConfiguration,
        symbolTable: SymbolTable,
        deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
    ) : this(
        symbolTable,
        errorCallback = { configuration.report(PartialLinkageDiagnostics.IR_LINKER_ERROR, it) },
        deserializedSymbolPostProcessor
    )

    val anyClass = symbolTable.referenceClass(StandardClassIds.Any.toIdSignature())
    val unitClass = symbolTable.referenceClass(StandardClassIds.Unit.toIdSignature())
    val nothingClass = symbolTable.referenceClass(StandardClassIds.Nothing.toIdSignature())

    val irInterner = IrInterningService()
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
                moduleDeserializer.fileDeserializers().firstOrNull { it.file == file }
            } ?: error("No file deserializer found for file: $file")

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

    open val partialLinkageSupport: PartialLinkageSupportForLinker get() = PartialLinkageSupportForLinker.DISABLED

    open val returnUnboundSymbolsIfSignatureNotFound: Boolean
        get() = partialLinkageSupport.isEnabled

    open val moduleDependencyTracker: IrModuleDependencyTracker get() = IrModuleDependencyTracker.DISABLED

    // Represents the fact that the linkage process was finished.
    // In case if a symbol was asked after deserialization, we would like to make sure that it is correctly and fully linked.
    private var finished = false

    fun deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
        idSignature: IdSignature,
        symbolKind: BinarySymbolData.SymbolKind,
        moduleDeserializer: IrModuleDeserializer,
    ): IrSymbol {
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
     *    ([IrModuleDeserializer.getDefinedPackageNames] returns `null`). These are checked for every package.
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
     *    ([IrModuleDeserializer.getDefinedPackageNames] returns `null`). These are checked for every package.
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

    override fun getSymbolAndPutIntoQueue(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind): IrSymbol? {
        val moduleDeserializer = resolveModuleDeserializer(signature) ?: return null
        return moduleDeserializer.tryDeserializeIrSymbol(signature, topLevelKindToSymbolKind(kind)).also {
            if (finished) deserializeAllReachableTopLevels()
        }
    }

    fun clear() {
        irInterner.reset()
    }

    override fun postProcess(irBuiltIns: IrBuiltIns, inOrAfterLinkageStep: Boolean) {
        finished = finished || inOrAfterLinkageStep
        if (inOrAfterLinkageStep) {
            // We have to exclude classifiers with unbound symbols in supertypes and in type parameter upper bounds from F.O. generation
            // to avoid failing with `Symbol for <signature> is unbound` error or generating fake overrides with incorrect signatures.
            partialLinkageSupport.exploreClassifiers(fakeOverrideBuilder)
            partialLinkageSupport.preprocessBeforeFakeOverridesBuilding(symbolTable, fakeOverrideBuilder)
        }

        // Fake override generator creates new IR declarations. This may have effect of binding for certain symbols.
        fakeOverrideBuilder.provideFakeOverrides(createTypeSystemContext(irBuiltIns))
        triedToDeserializeDeclarationForSymbol.clear()

        if (inOrAfterLinkageStep) {
            // Finally, generate stubs for the remaining unbound symbols and patch every usage of any unbound symbol inside the IR tree.
            partialLinkageSupport.generateStubsAndPatchUsages(irBuiltIns, symbolTable)
            allModuleDeserializers.forEach { if (it is IrModuleDeserializerWithBuiltIns) it.finish(irBuiltIns) }
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

    fun createAndRegisterModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        kotlinLibrary: KotlinLibrary?,
        strategyResolver: DeserializationStrategyResolver,
    ): IrModuleDeserializer {
        val moduleFragment = IrModuleFragmentImpl(moduleDescriptor)
        moduleFragment.kotlinLibrary = kotlinLibrary

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

        val definedPackageNames = deserializer.getDefinedPackageNames()
        if (definedPackageNames == null) {
            moduleDeserializersWithUnknownPackageNames += deserializer
        } else {
            for (packageName in definedPackageNames) {
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

    fun getAllMatchingSignatures(callableId: CallableId, signatureKind: IrDeserializer.TopLevelSymbolKind): List<IdSignature> {
        return allModuleDeserializers.flatMap { moduleDeserializer ->
            moduleDeserializer.getAllMatchingSignatures(callableId, signatureKind)
        }
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
 * declaration is always deserialized on demand. The strategy decides what is loaded *in addition* to that,
 * and whether function bodies are loaded.
 *
 * @property onDemand The file is not added to [IrModuleFragment.files] and is not reported among
 *   [IrModuleDeserializer.fileDeserializers]. Its declarations are deserialized only when something refers to them.
 *   For example, when Kotlin/Native builds a per-file cache for a single file of `stdlib`, all other files
 *   of `stdlib` are [onDemand]: they must not get into the module fragment (otherwise code would be generated
 *   for them as well), but a function from such a file that is called from the cached file still has to be
 *   resolvable.
 * @property needBodies Bodies of all deserialized functions are loaded. For example, the main module compiled
 *   to JavaScript needs every function body, because the backend generates code for all of them.
 * @property explicitlyExported Declarations that the serializer recorded as explicitly exported to the compiler
 *   are deserialized even if nothing refers to them. These are the roots required by the platform rather than by
 *   Kotlin code: `@JsExport` declarations on JS, or functions annotated with `@ExportForCppRuntime` on Native that
 *   are called from the C++ runtime. Without this flag, a `@JsExport fun greet()` in a dependency that no Kotlin
 *   code calls would be silently dropped from the resulting JS module.
 * @property theWholeWorld All top-level declarations of the file are deserialized, whether referenced or not.
 *   For example, the `dump-ir` command of the `klib` tool must show every declaration of the library, not only
 *   those reachable from somewhere.
 * @property inlineBodies Bodies of inline functions (and of local functions declared inside them) are loaded,
 *   even if [needBodies] is not set. For example, when user code calls `list.map { it * 2 }`, the backend has to
 *   inline the body of `kotlin.collections.map` into the call site, so it needs that body even if `stdlib` itself
 *   is not going to be compiled.
 */
enum class DeserializationStrategy(
    val onDemand: Boolean = false,
    val needBodies: Boolean = false,
    val explicitlyExported: Boolean = false,
    val theWholeWorld: Boolean = false,
    val inlineBodies: Boolean = false,
) : DeserializationStrategyResolver {
    /**
     * The file is hidden from the module fragment. Its declarations are deserialized only when referenced,
     * and without function bodies.
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
    ALL(needBodies = true, explicitlyExported = true, theWholeWorld = true, inlineBodies = true),

    /**
     * Only declarations reachable from elsewhere, plus explicitly exported ones, are deserialized,
     * and all of them get their function bodies.
     *
     * Used for regular dependencies in whole-program compilation (Kotlin/Native, Kotlin/JS): if the
     * program calls only `println()` from `stdlib`, then only `println()` and whatever it transitively
     * refers to are loaded and compiled, together with the platform roots like `@JsExport` declarations.
     */
    EXPLICITLY_EXPORTED(needBodies = true, explicitlyExported = true, inlineBodies = true),

    /**
     * Only declarations reachable from elsewhere are deserialized, without any function bodies.
     *
     * Used when the code of a library is already compiled and only its declarations need to be
     * known for linkage, e.g. a library that is fully present in the Kotlin/Native cache. This is also
     * the default strategy of [IrModuleDeserializer].
     */
    ONLY_DECLARATION_HEADERS,

    /**
     * Only declarations reachable from elsewhere are deserialized, and only inline functions get their bodies.
     *
     * Used when a library is not going to be compiled, but its inline functions still need to be inlined
     * into the code that is, e.g. unmodified files during JS incremental compilation, or a Kotlin/Native
     * library that is cached as a header cache.
     */
    WITH_INLINE_BODIES(inlineBodies = true);

    override fun invoke(filePath: String) = this
}

fun interface DeserializationStrategyResolver {
    operator fun invoke(filePath: String): DeserializationStrategy
}

/**
 * This is an auxiliary attribute that is used to store [KotlinLibrary] instance for deserialized [IrModuleFragment].
 *
 * It is stamped by [KotlinIrLinker] for every module fragment created during IR linkage. Code that creates a synthetic
 * module fragment for an already deserialized library module must stamp the new fragment manually.
 *
 * The attribute stays `null` for module fragments that the linker did not create: the fragment of the module being
 * compiled, fragments on backends that have no klibs at all (JVM), and the single fragment that the frontend creates
 * for all binary dependencies at once, see `DependencyListForCliModule.Builder`.
 */
var IrModuleFragment.kotlinLibrary: KotlinLibrary? by irAttribute(copyByDefault = false)
