/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.api.platform.declarations.mergeDeclarationProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.mergePackageProviders
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.getTopLevelCallables
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider
import org.jetbrains.kotlin.fir.java.hasMetadataAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.reflect.KClass

internal class LLModuleWithDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
    val computeDependencyProviders: () -> List<FirSymbolProvider>,
) : FirSymbolProvider(session) {
    private val module = session.llFirModuleData.ktModule

    val dependencyProviders: List<FirSymbolProvider> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computeDependencyProviders().also { providers ->
            require(providers.all { it !is LLModuleWithDependenciesSymbolProvider }) {
                "${LLModuleWithDependenciesSymbolProvider::class.simpleName} may not contain ${LLModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                        " dependency providers must be flattened during session creation."
            }
        }
    }

    // We need this separate symbol provider because everything here is lazy.... :(
    // Lazily calculating everything in this `LLFirModuleWithDependenciesSymbolProvider` would be a mess.
    private val underlyingSymbolProvider: FirSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Unified symbol providers aren't supported for resolvable library sessions because the `KaModule`-based selection mechanism
        // doesn't work with rest library symbol providers.
        // TODO (marco): This would be a NONISSUE if the rest libraries symbol provider wasn't a regular Java symbol provider...
        if (
            session is LLFirLibraryOrLibrarySourceResolvableModuleSession ||
            module is KaDanglingFileModule && (module.contextModule is KaLibraryModule || module.contextModule is KaLibrarySourceModule)
        ) {
            return@lazy FirCompositeSymbolProvider(session, providers + dependencyProviders)
        }

        LLUnifiedSymbolProvider(session, module.project, providers, dependencyProviders, providers + dependencyProviders)
    }

    /**
     * This symbol names provider is not used directly by [LLModuleWithDependenciesSymbolProvider], because in the IDE, Java symbol
     * providers currently cannot provide name sets (see KTIJ-24642). So in most cases, name sets would be `null` anyway.
     *
     * However, in Standalone mode, we rely on the symbol names provider to compute classifier/callable name sets for package scopes (see
     * `DeclarationsInPackageProvider`). The fallback declaration provider doesn't work for symbols from binary libraries.
     *
     * [symbolNamesProvider] needs to be lazy to avoid eager initialization of [LLDependenciesSymbolProvider.providers].
     */
    override val symbolNamesProvider: FirSymbolNamesProvider
        get() = underlyingSymbolProvider.symbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        underlyingSymbolProvider.getClassLikeSymbolByClassId(classId)

    @OptIn(FirSymbolProviderInternals::class)
    fun getDeserializedClassLikeSymbolByClassIdWithoutDependencies(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): FirClassLikeSymbol<*>? = providers.firstNotNullOfOrNull { provider ->
        when (provider) {
            is LLKotlinStubBasedLibrarySymbolProvider -> provider.getClassLikeSymbolByClassId(classId, classLikeDeclaration)
            is AbstractFirDeserializedSymbolProvider -> provider.getClassLikeSymbolByClassId(classId)
            else -> null
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        underlyingSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    private val multifileClassPartCallableSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLKotlinStubBasedLibraryMultifileClassPartCallableSymbolProvider(session)
    }

    @FirSymbolProviderInternals
    fun getTopLevelDeserializedCallableSymbolsToWithoutDependencies(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: KtCallableDeclaration,
    ) {
        val sizeBefore = destination.size

        providers.forEach { provider ->
            when (provider) {
                is LLKotlinStubBasedLibrarySymbolProvider ->
                    destination.addIfNotNull(provider.getTopLevelCallableSymbol(packageFqName, shortName, callableDeclaration))

                is AbstractFirDeserializedSymbolProvider ->
                    provider.getTopLevelCallableSymbolsTo(destination, packageFqName, shortName)

                else -> {}
            }
        }

        // Must be called after the original search as this is only a fallback solution
        if (sizeBefore == destination.size && providers.any { it is LLKotlinStubBasedLibrarySymbolProvider }) {
            multifileClassPartCallableSymbolProvider.addCallableIfNeeded(destination, packageFqName, shortName, callableDeclaration)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        underlyingSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        underlyingSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    override fun hasPackage(fqName: FqName): Boolean = underlyingSymbolProvider.hasPackage(fqName)

    fun hasPackageWithoutDependencies(fqName: FqName): Boolean = providers.any { it.hasPackage(fqName) }

    // TODO (marco): A view on this symbol provider which only provides dependencies. Needs to bypass caches, or separate caches, or try the
    //               cache and filter the result then fall back to indices (probably the smartest move). Currently, this is a naive
    //               implementation which essentially duplicates the unified symbol provider!
    //               `dependenciesSymbolProvider` is used heavily in dangling file sessions as the dependency symbol provider.
    val dependenciesSymbolProvider: FirSymbolProvider = object : FirSymbolProvider(session) {
        /**
         * [dependenciesSymbolProvider] is accessed during session creation, so the calculation of the [dependencyProviders] must be lazy.
         * Hence, we cannot expose the unified symbol provider directly as the [dependenciesSymbolProvider].
         */
        private val underlyingSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
            LLUnifiedSymbolProvider(
                session,
                module.project,
                dependencyProviders,
                emptyList(),
                dependencyProviders,
            )
        }

        override val symbolNamesProvider: FirSymbolNamesProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
            underlyingSymbolProvider.symbolNamesProvider
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
            return underlyingSymbolProvider.getClassLikeSymbolByClassId(classId)
        }

        @FirSymbolProviderInternals
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<FirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
            underlyingSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }

        @FirSymbolProviderInternals
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<FirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            underlyingSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }

        @FirSymbolProviderInternals
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<FirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            underlyingSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }

        override fun hasPackage(fqName: FqName): Boolean = underlyingSymbolProvider.hasPackage(fqName)
    }
}

// TODO (marco): Document. Basic idea: combine own providers and dependency providers into a single unified symbol provider.
@OptIn(FirSymbolProviderInternals::class)
private class LLUnifiedSymbolProvider(
    session: FirSession,
    project: Project,
    private val providers: List<FirSymbolProvider>,
    val dependencyProviders: List<FirSymbolProvider>,
    private val allProviders: List<FirSymbolProvider>,
) : FirSymbolProvider(session) {
    // TODO (marco): Optimize!
    override val symbolNamesProvider = FirCompositeCachedSymbolNamesProvider(
        session,
        allProviders.map { it.symbolNamesProvider },
    )

    // TODO (marco): This is ugly with all the `java*` and `kotlin*` properties. Maybe we can have a "combined symbol provider-ish"
    //               component which takes care of this? Essentially, untangle. As we are also not guaranteed to have Java symbol providers
    //               at all! Likely: roll into `LLCandidateSelector`.

    // TODO (marco): Add stub-based deserialized symbol providers (needs the optimized scope first).
    // Unify own and dependency providers, disambiguate based on `KaModule`
    val kotlinSymbolProviders = providers.filterIsInstance<LLKotlinSymbolProvider>() +
            dependencyProviders.filterIsInstance<LLKotlinSymbolProvider>()

    val kotlinDeclarationProvider = project.mergeDeclarationProviders(kotlinSymbolProviders.map { it.declarationProvider })

    val kotlinPackageProvider = project.mergePackageProviders(kotlinSymbolProviders.map { it.packageProvider })

    val kotlinPackageProviderForKotlinPackages = kotlinSymbolProviders
        .filter { it.allowKotlinPackage }
        .takeIf { it.isNotEmpty() }
        ?.map { it.packageProvider }
        ?.let(project::mergePackageProviders)

    val kotlinCandidateSelector = LLCandidateSelector<KtClassLikeDeclaration, LLKotlinSymbolProvider>(
        session,
        LLKotlinSymbolProvider::class,
        allProviders,
    )

    // This only regards OWN providers. Otherwise, we might end the search early in e.g. a dependency Kotlin provider while there's still an
    // own Java provider to check.
    val highestOwnKotlinPrecedence = providers.indexOfFirst { it is LLKotlinSymbolProvider }

    val javaSymbolProviders = providers.filterIsInstance<LLFirJavaSymbolProvider>() +
            dependencyProviders.filterIsInstance<LLFirJavaSymbolProvider>()

    val javaClassFinder = run {
        val combinedScope = KotlinGlobalSearchScopeMerger.getInstance(project).union(javaSymbolProviders.map { it.searchScope })
        project.createJavaClassFinder(combinedScope)
    }

    val javaCandidateSelector = LLCandidateSelector<JavaClass, LLFirJavaSymbolProvider>(
        session,
        LLFirJavaSymbolProvider::class,
        allProviders,
    )

    val highestOwnJavaPrecedence = providers.indexOfFirst { it is LLFirJavaSymbolProvider }

    val restProviders = providers.filterNot { it is LLKotlinSymbolProvider || it is LLFirJavaSymbolProvider }
    val restDependencyProviders = dependencyProviders.filterNot { it is LLKotlinSymbolProvider || it is LLFirJavaSymbolProvider }

    private val classifierCache = NullableCaffeineCache<ClassId, FirClassLikeSymbol<*>> { it.maximumSize(3000) }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return classifierCache.get(classId) { computeClassLikeSymbolByClassId(it) }
    }

    private fun computeClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        // We have to access the uncombined own providers first, since Kotlin/Java symbol provider access is unified between own and
        // dependency providers.
        restProviders.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }?.let { return it }

        // TODO (marco): If the Kotlin package provider is empty, don't hit the index for "kotlin" package classes.
        val kotlinCandidates =
            kotlinDeclarationProvider.getAllClassesByClassId(classId) +
                    kotlinDeclarationProvider.getAllTypeAliasesByClassId(classId)
        val kotlinResult = kotlinCandidateSelector.selectFirstElementInClasspathOrder(kotlinCandidates) { it }

        // Fast path: The candidate was found in Kotlin own providers. We don't need to check Java providers at all.
        // NOTE: This only works because symbol providers are independent. Otherwise, we don't know if the Kotlin or Java provider is
        // ordered first.
        if (kotlinResult != null && kotlinResult.precedence == highestOwnKotlinPrecedence) {
            return kotlinResult.provider.getClassLikeSymbolByClassId(classId, kotlinResult.candidate)
        }

        // TODO (marco): Don't hit the index for "kotlin" package classes. (Rather, introduce a fast path which only looks at
        //               Kotlin package-providable providers.)
        val javaCandidates = javaClassFinder.findClasses(classId).filterNot(JavaClass::hasMetadataAnnotation)
        val javaResult = javaCandidateSelector.selectFirstElementInClasspathOrder(javaCandidates) { javaClass ->
            // `JavaClass` doesn't know anything about PSI, but we can be sure that `findClasses` returns a `JavaClassImpl` because it's
            // using `KotlinJavaPsiFacade`. The alternative to this hack would be to change the interface of either `JavaClass` (yet the
            // module should hardly depend on PSI), or to have `KotlinJavaPsiFacade` and `JavaClassFinderImpl` return `JavaClassImpl` and to
            // return `JavaClassFinderImpl` from `createJavaClassFinder`.
            check(javaClass is JavaClassImpl) { "`findClasses` as used here should return `JavaClassImpl` results." }
            javaClass.psi
        }

        // Fast path: The candidate was found in Java own providers. We don't need to check the result against the Kotlin result.
        if (javaResult != null && javaResult.precedence == highestOwnJavaPrecedence) {
            return javaResult.provider.getClassLikeSymbolByClassId(classId, javaResult.candidate)
        }

        val higherResult = if (javaResult != null && kotlinResult != null) {
            if (kotlinResult.precedence < javaResult.precedence) kotlinResult else javaResult
        } else kotlinResult ?: javaResult

        higherResult?.let { return it.toClassLikeSymbol(classId) }

        // If we haven't found the class by now, we still have the uncombined dependency symbol providers to go through.
        return restDependencyProviders.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        restProviders.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }

        // Java symbol providers don't provide any callables.
        kotlinCandidateSelector.forEachCallableProvider(
            packageFqName,
            name,
            kotlinDeclarationProvider::getTopLevelCallables,
        ) { callableId, callables ->
            getTopLevelCallableSymbolsTo(destination, callableId, callables)
        }

        restDependencyProviders.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        restProviders.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }

        kotlinCandidateSelector.forEachCallableProvider(
            packageFqName,
            name,
            kotlinDeclarationProvider::getTopLevelFunctions,
        ) { callableId, functions ->
            getTopLevelFunctionSymbolsTo(destination, callableId, functions)
        }

        restDependencyProviders.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        restProviders.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }

        kotlinCandidateSelector.forEachCallableProvider(
            packageFqName,
            name,
            kotlinDeclarationProvider::getTopLevelProperties,
        ) { callableId, properties ->
            getTopLevelPropertySymbolsTo(destination, callableId, properties)
        }

        restDependencyProviders.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        if (restProviders.any { it.hasPackage(fqName) }) return true

        // TODO (marco): Untangle.
        val hasKotlinPackage = if (fqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) {
            // If a package is a `kotlin` package, `packageProvider` might find it via the scope of an individual symbol provider that
            // disallows `kotlin` packages. Hence, the combined `getPackage` would erroneously find a package it shouldn't be able to find,
            // because calling that individual symbol provider directly would result in `null` (as it disallows `kotlin` packages). The
            // `packageProviderForKotlinPackages` solves this issue by including only scopes from symbol providers which allow `kotlin`
            // packages.
            kotlinPackageProviderForKotlinPackages?.doesKotlinOnlyPackageExist(fqName) == true
        } else {
            kotlinPackageProvider.doesKotlinOnlyPackageExist(fqName)
        }
        if (hasKotlinPackage) return true

        if (javaSymbolProviders.any { it.hasPackage(fqName) }) return true

        return restDependencyProviders.any { it.hasPackage(fqName) }
    }
}

// TODO (marco): Copied from selecting combined symbol provider.

// Keeps tally of the precedence of ALL providers, but only serves one class of candidates (Kotlin or Java). We cannot have a unified
// selection without additional machinery because Kotlin and Java classes might have the same `KaModule`.
private class LLCandidateSelector<CANDIDATE, PROVIDER : LLKnownClassDeclarationSymbolProvider<CANDIDATE>>(
    session: FirSession,
    private val providerClass: KClass<*>,
    private val allProviders: List<FirSymbolProvider>,
) {
    private val providers = allProviders.filter { providerClass.isInstance(it) }

    /**
     * [KaModule] precedence must be checked in case of multiple candidates to preserve classpath order.
     */
    private val modulePrecedenceMap: Map<KaModule, Int> = buildMap {
        allProviders.forEachIndexed { index, provider ->
            if (!providerClass.isInstance(provider)) return@forEachIndexed

            val module = provider.session.llFirModuleData.ktModule
            if (module in this) {
                error("Expected all symbol providers to have unique modules, but the following module is not unique: $module")
            }
            put(module, index)
        }
    }

    private val contextualModule = session.llFirModuleData.ktModule

    private val symbolNamesProvider: FirSymbolNamesProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FirCompositeCachedSymbolNamesProvider.fromSymbolProviders(session, providers)
    }

    /**
     * Cache [KotlinProjectStructureProvider] to avoid service access when getting [KaModule]s.
     */
    @KaCachedService
    private val projectStructureProvider: KotlinProjectStructureProvider =
        KotlinProjectStructureProvider.getInstance(contextualModule.project)

    class ClassSearchResult<C, P : LLKnownClassDeclarationSymbolProvider<C>>(
        val candidate: C,
        val provider: P,
        val precedence: Int,
    ) {
        @OptIn(FirSymbolProviderInternals::class)
        fun toClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? = provider.getClassLikeSymbolByClassId(classId, candidate)
    }

    // TODO (marco): update documentation. We need to return the precedence to compare results from different `LLCandidateSelector`s.
    /**
     * Selects the element with the highest module precedence in [candidates], returning the element and the provider to which resolution
     * should be delegated. This is a post-processing step that preserves classpath order when, for example, an index access with a combined
     * scope isn't guaranteed to return the first element in classpath order.
     */
    fun selectFirstElementInClasspathOrder(
        candidates: Collection<CANDIDATE>,
        getElement: (CANDIDATE) -> PsiElement?,
    ): ClassSearchResult<CANDIDATE, PROVIDER>? {
        if (candidates.isEmpty()) return null

        // We're using a custom implementation instead of `minBy` so that `ktModule` doesn't need to be fetched twice.
        var currentCandidate: CANDIDATE? = null
        var currentPrecedence: Int = Int.MAX_VALUE

        for (candidate in candidates) {
            val element = getElement(candidate) ?: continue
            val ktModule = projectStructureProvider.getModule(element, contextualModule)

            // If `ktModule` cannot be found in the map, `candidate` cannot be processed by any of the available providers, because none of
            // them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to any
            // results for `candidate`.
            val precedence = modulePrecedenceMap[ktModule] ?: continue
            if (precedence < currentPrecedence) {
                currentCandidate = candidate
                currentPrecedence = precedence
            }
        }

        val candidate = currentCandidate ?: return null

        // TODO (marco): Update comment. We can search the provider by precedence in the all provider map since `modulePrecedenceMap`
        //               sources the precedence numbers from each provider's index.
        // The provider will always be found at this point, because `modulePrecedenceMap` contains the same keys as `providersByKtModule`
        // and a precedence for `currentKtModule` must have been found in the previous step.
        @Suppress("UNCHECKED_CAST")
        val provider = allProviders[currentPrecedence] as PROVIDER

        return ClassSearchResult(candidate, provider, currentPrecedence)
    }

    // TODO (marco): Instead of this, each unified symbol provider should just implement `getTopLevelCallableSymbolsTo` and so on. Since we
    //               need to find ALL callables, the interface doesn't need to expose any kind of precedence.
    /**
     * Calls [provide] on those providers which can contribute a callable of the given name.
     */
    inline fun <A : KtCallableDeclaration> forEachCallableProvider(
        packageFqName: FqName,
        name: Name,
        getCallables: (CallableId) -> Collection<A>,
        provide: PROVIDER.(CallableId, Collection<A>) -> Unit,
    ) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)

        getCallables(callableId)
            .groupBy { projectStructureProvider.getModule(it, contextualModule) }
            .forEach { (module, callables) ->
                // If `module` cannot be found in the map, `callables` cannot be processed by any of the available providers, because none
                // of them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to
                // any results for `callables`.
                val provider = modulePrecedenceMap[module]?.let { allProviders[it] } ?: return@forEach

                @Suppress("UNCHECKED_CAST")
                (provider as PROVIDER).provide(callableId, callables)
            }
    }
}

/*
internal class LLFirDependenciesSymbolProvider(
    session: FirSession,
    val computeProviders: () -> List<FirSymbolProvider>,
) : FirSymbolProvider(session) {
    /**
     * Dependency symbol providers are lazy to support cyclic dependencies between modules. If a module A and a module B depend on each
     * other and session creation tries to access dependency symbol providers eagerly, the creation of session A would try to create session
     * B (to get its symbol providers), which in turn would try to create session A, and so on.
     */
    val providers: List<FirSymbolProvider> by lazy {
        computeProviders().also { providers ->
            require(providers.all { it !is LLModuleWithDependenciesSymbolProvider }) {
                "${LLDependenciesSymbolProvider::class.simpleName} may not contain ${LLModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                        " dependency providers must be flattened during session creation."
            }
        }
    }

    private val expectBuiltinPostProcessor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ExpectBuiltinPostProcessor.createIfNeeded(session, providers)
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = FirNullSymbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        providers
            .firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
            ?.let { expectBuiltinPostProcessor?.actualizeExpectBuiltin(classId, it) ?: it }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelCallableSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelFunctionSymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val facades = SmartSet.create<JvmClassName>()
        for (provider in providers) {
            val newSymbols = buildSmartList {
                provider.getTopLevelPropertySymbolsTo(this, packageFqName, name)
            }
            addNewSymbolsConsideringJvmFacades(destination, newSymbols, facades)
        }
    }

    override fun hasPackage(fqName: FqName): Boolean = providers.any { it.hasPackage(fqName) }

    private fun <S : FirCallableSymbol<*>> addNewSymbolsConsideringJvmFacades(
        destination: MutableList<S>,
        newSymbols: List<S>,
        facades: MutableSet<JvmClassName>,
    ) {
        if (newSymbols.isEmpty()) return
        val newFacades = SmartSet.create<JvmClassName>()
        for (symbol in newSymbols) {
            val facade = symbol.jvmClassNameIfDeserialized()
            if (facade != null) {
                newFacades += facade
                if (facade !in facades) {
                    destination += symbol
                }
            } else {
                destination += symbol
            }
        }
        facades += newFacades
    }
}

/**
 * [ExpectBuiltinPostProcessor] is a workaround for KT-72390.
 *
 * The core of the issue is an incorrect ordering of symbol providers/dependencies: The common stdlib now contains `expect` declarations for
 * builtins, while the JVM stdlib doesn't yet contain the `actual` counterparts. Because the common stdlib is ordered before the JVM
 * builtins provider, symbol providers return `expect` classes for builtins instead of the `actual` builtins.
 *
 * The workaround is expected to become unnecessary with KT-68154, when `actual` builtins become part of the JVM stdlib.
 */
private class ExpectBuiltinPostProcessor(private val builtinSymbolProvider: FirSymbolProvider) {
    fun actualizeExpectBuiltin(classId: ClassId, symbol: FirClassLikeSymbol<*>): FirClassLikeSymbol<*> {
        if (!symbol.isExpect) return symbol
        if (!classId.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) return symbol
        if (!symbol.annotations.any(::isActualizeBuiltinsAnnotation)) return symbol

        // Prefer the `actual` JVM builtin over the `expect` builtin provided by the common stdlib.
        return builtinSymbolProvider.getClassLikeSymbolByClassId(classId) ?: symbol
    }

    private fun isActualizeBuiltinsAnnotation(annotation: FirAnnotation): Boolean {
        if (annotation !is FirAnnotationCall) return false
        val reference = annotation.calleeReference as? FirNamedReference ?: return false

        // We avoid resolving the `@ActualizeByJvmBuiltinProvider` annotation here and just check its simple name. Resolution inside symbol
        // providers is generally problematic, and we want to avoid it. This approach is technically not correct, but the following
        // combination of parameters gives us a certain footing:
        //
        // - The post-processor is only used when the session has a dependency with `stdlibCompilation`, which means it's limited to the
        //   `kotlin` project.
        // - It is limited to symbols in the `kotlin.*` package, which will not usually be declared in the wild and not even in most of the
        //   compiler code (which uses `org.jetbrains.kotlin.*`).
        // - The name `ActualizeByJvmBuiltinProvider` is sufficiently specific to avoid ambiguities.
        return reference.name == StandardClassIds.Annotations.ActualizeByJvmBuiltinProvider.shortClassName
    }

    companion object {
        private val isEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.jvmBuiltinActualizationForStdlibSources", true)
        }

        /**
         * Creates an [ExpectBuiltinPostProcessor] only if it's needed. In general, this means:
         *
         * - The workaround is enabled in the registry (`true` by default).
         * - The post-processor is only needed for source and script modules, since other kinds of modules such as libraries cannot depend
         *   on stdlib sources. We have to make a special provision for dangling files, which get their platform and language version
         *   settings from the context session.
         * - It is only needed for JVM platform sessions, since we specifically want to actualize JVM builtins.
         * - It is only needed for the `kotlin` project, since only projects which use the stdlib from *sources* are affected. Conveniently,
         *   `stdlib` modules have the `-Xstdlib-compilation` flag set.
         */
        fun createIfNeeded(session: FirSession, dependencyProviders: List<FirSymbolProvider>): ExpectBuiltinPostProcessor? {
            if (!isEnabled) return null

            val module = session.llFirModuleData.ktModule
            if (module !is KaSourceModule && module !is KaScriptModule && module !is KaDanglingFileModule) return null

            if (!session.llFirModuleData.platform.isJvm()) return null
            if (dependencyProviders.none { it.hasStdlibSourceSession }) return null

            val builtinSymbolProvider = searchBuiltinSymbolProvider(dependencyProviders) ?: return null
            return ExpectBuiltinPostProcessor(builtinSymbolProvider)
        }

        private val FirSymbolProvider.hasStdlibSourceSession: Boolean
            get() = when (this) {
                is LLCombinedSymbolProvider<*> -> providers.any { it.hasStdlibSourceSession }
                is FirCompositeSymbolProvider -> providers.any { it.hasStdlibSourceSession }
                else -> session.isStdlibSourceSession
            }

        private val FirSession.isStdlibSourceSession: Boolean
            get() = languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)

        private fun searchBuiltinSymbolProvider(providers: List<FirSymbolProvider>): FirSymbolProvider? =
            providers.firstNotNullOfOrNull { provider ->
                when (provider) {
                    is FirCompositeSymbolProvider -> searchBuiltinSymbolProvider(provider.providers)

                    // Standalone uses `FirFallbackBuiltinSymbolProvider`, while LL-specific builtin symbol providers are marked with
                    // `LLBuiltinSymbolProviderMarker`.
                    is FirFallbackBuiltinSymbolProvider, is LLBuiltinSymbolProviderMarker -> provider

                    else -> null
                }
            }
    }
}
*/

/**
 * Every [LLFirSession] has [LLModuleWithDependenciesSymbolProvider] as a symbol provider
 */
internal val LLFirSession.symbolProvider: LLModuleWithDependenciesSymbolProvider
    get() = (this as FirSession).symbolProvider as LLModuleWithDependenciesSymbolProvider
