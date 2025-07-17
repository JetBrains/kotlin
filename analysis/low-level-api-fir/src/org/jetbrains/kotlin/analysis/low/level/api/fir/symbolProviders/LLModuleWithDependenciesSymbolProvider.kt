/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * The module-level [FirSymbolProvider] for an [LLFirSession], composing multiple kinds of symbol providers for the module's own content
 * ([providers]) and its dependencies ([dependencyProvider]).
 *
 * This class does not implement [LLPsiAwareSymbolProvider] because for specific-module symbol provider access, only own symbol providers
 * should be considered, but not dependencies. [LLModuleWithDependenciesSymbolProvider] must consider dependencies for all its overridden
 * symbol provider functions. As such, the class offers alternative functions like [getClassLikeSymbolByPsiWithoutDependencies].
 *
 * ### Module content inclusion
 *
 * [LLModuleWithDependenciesSymbolProvider] must and must only provide symbols for declarations in the associated module's content scope.
 *
 * In general, the following statements should all be consistent with each other for a given `declaration` and `module`:
 *
 * - `declaration` is included in `module`’s content scope.
 * - The project structure provider provides `module` for `declaration` (or chooses a competing, equally valid candidate based on use-site
 *   module disambiguation).
 * - The symbol provider for `module`'s FIR session provides a symbol for `declaration`.
 *
 * Content scopes are the source of truth in this matter. As such, the implementation of the symbol provider must be consistent with the
 * content scope. This is not always intuitive. For example, the content scope of a library module may exclude certain files from the
 * library which are nonetheless physically present in an underlying JAR.
 *
 * [KotlinProjectStructureProvider][org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider] has the same
 * responsibility, which is the burden of the Analysis API platform.
 */
internal class LLModuleWithDependenciesSymbolProvider(
    session: LLFirSession,
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

    @LLModuleSpecificSymbolProviderAccess
    fun getClassLikeSymbolByPsiWithoutDependencies(
        classId: ClassId,
        declaration: PsiElement,
    ): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolMatchingPsi(classId, declaration) }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    private val multifileClassPartCallableSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLKotlinStubBasedLibraryMultifileClassPartCallableSymbolProvider(session)
    }

    @OptIn(FirSymbolProviderInternals::class)
    fun getTopLevelDeserializedCallableSymbolsWithoutDependencies(
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: KtCallableDeclaration,
    ): List<FirCallableSymbol<*>> = buildList {
        providers.forEach { provider ->
            when (provider) {
                is LLKotlinStubBasedLibrarySymbolProvider ->
                    addIfNotNull(provider.getTopLevelCallableSymbol(packageFqName, shortName, callableDeclaration))

                is AbstractFirDeserializedSymbolProvider ->
                    provider.getTopLevelCallableSymbolsTo(this, packageFqName, shortName)

                else -> {}
            }
        }

        // Must be called after the original search as this is only a fallback solution
        if (isEmpty() && providers.any { it is LLKotlinStubBasedLibrarySymbolProvider }) {
            multifileClassPartCallableSymbolProvider.addCallableIfNeeded(this, packageFqName, shortName, callableDeclaration)
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

    override val symbolNamesProvider: FirSymbolNamesProvider = FirNullSymbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

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

    /**
     * Filters out any top-level [FirCallableSymbol]s defined in a facade class which have already been added by a previous symbol provider
     * for a facade class with the same name.
     *
     * We need this handling because we usually have two sources for builtins:
     *
     * 1. The builtins from the stdlib library symbol provider.
     * 2. The fallback builtins from the symbol provider created in
     *    [LLFirBuiltinsSessionFactory][org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory].
     *
     * In contrast to class symbols where we return the first match, for callables we query all symbol providers and build a list of all
     * candidates. Callables declared in the same facade class, but provided by two different symbol providers, are essentially duplicates.
     * Hence, we filter them out.
     *
     * Regarding builtins, this logic filters out callables from fallback builtins if they have already been added from the stdlib, since
     * the fallback builtins provider is ordered last.
     */
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
