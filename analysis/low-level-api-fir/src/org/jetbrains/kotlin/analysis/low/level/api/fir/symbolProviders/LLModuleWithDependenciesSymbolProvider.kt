/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedSymbolProvider
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

internal class LLModuleWithDependenciesSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
    val dependencyProvider: LLDependenciesSymbolProvider,
) : FirSymbolProvider(session) {
    /**
     * This symbol names provider is not used directly by [LLModuleWithDependenciesSymbolProvider], because in the IDE, Java symbol
     * providers currently cannot provide name sets (see KTIJ-24642). So in most cases, name sets would be `null` anyway.
     *
     * However, in Standalone mode, we rely on the symbol names provider to compute classifier/callable name sets for package scopes (see
     * `DeclarationsInPackageProvider`). The fallback declaration provider doesn't work for symbols from binary libraries.
     *
     * [symbolNamesProvider] needs to be lazy to avoid eager initialization of [LLDependenciesSymbolProvider.providers].
     */
    override val symbolNamesProvider: FirSymbolNamesProvider by lazy {
        FirCompositeCachedSymbolNamesProvider(
            session,
            buildList {
                providers.mapTo(this) { it.symbolNamesProvider }
                dependencyProvider.providers.mapTo(this) { it.symbolNamesProvider }
            },
        )
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassId(classId)

    fun getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

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
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
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
        getTopLevelFunctionSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelPropertySymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    @FirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutDependencies(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name
    ) {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean =
        hasPackageWithoutDependencies(fqName)
                || dependencyProvider.hasPackage(fqName)

    fun hasPackageWithoutDependencies(fqName: FqName): Boolean =
        providers.any { it.hasPackage(fqName) }
}

internal class LLDependenciesSymbolProvider(
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

/**
 * Every [LLFirSession] has [LLModuleWithDependenciesSymbolProvider] as a symbol provider
 */
internal val LLFirSession.symbolProvider: LLModuleWithDependenciesSymbolProvider
    get() = (this as FirSession).symbolProvider as LLModuleWithDependenciesSymbolProvider
