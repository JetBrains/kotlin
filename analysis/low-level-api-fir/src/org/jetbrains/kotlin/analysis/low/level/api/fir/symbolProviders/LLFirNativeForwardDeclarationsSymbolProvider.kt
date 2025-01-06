/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createForwardDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createForwardDeclarationsPackageProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.createSyntheticForwardDeclarationClass
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Symbol provider for synthetic Kotlin/Native forward declarations.
 * Creation of a symbol is shared between the Analysis API and the CLI implementations.
 * The major difference is that [declarationProvider] should be able to find a source declaration, otherwise, the symbol won't be created.
 * The declaration found by the [declarationProvider] is written as the symbol's source.
 */
internal class LLFirNativeForwardDeclarationsSymbolProvider(
    session: LLFirSession,
    override val declarationProvider: KotlinDeclarationProvider,
    override val packageProvider: KotlinPackageProvider,
) : LLFirKotlinSymbolProvider(session) {
    private val moduleData: LLFirModuleData get() = session.llFirModuleData

    /**
     * Forward declarations are not defined in `kotlin` package
     */
    override val allowKotlinPackage: Boolean get() = false

    override val symbolNamesProvider: FirSymbolNamesProvider =
        LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider, allowKotlinPackage)

    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, KtClassLikeDeclaration?> =
        session.firCachesFactory.createCache(
            createValue = createValue@{ classId: ClassId, contextDeclaration: KtClassLikeDeclaration? ->
                val declaration = contextDeclaration
                    ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
                    ?: return@createValue null

                createSyntheticForwardDeclarationClass(classId, moduleData, this.session, this.session.kotlinScopeProvider) {
                    source = KtRealPsiSourceElement(declaration)
                }
            }
        )

    override fun hasPackage(fqName: FqName): Boolean = packageProvider.doesKotlinOnlyPackageExist(fqName)

    @FirSymbolProviderInternals
    override fun getClassLikeSymbolByClassId(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): FirClassLikeSymbol<*>? {
        return classCache.getValue(classId, classLikeDeclaration)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.getValue(classId, null)
    }

    // Region: no-op overrides for symbols that don't exist in K/N forward declarations

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>, callableId: CallableId, callables: Collection<KtCallableDeclaration>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>, callableId: CallableId, functions: Collection<KtNamedFunction>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>, callableId: CallableId, properties: Collection<KtProperty>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name,
    ) {
    }

    // endregion
}

/**
 * Creates a [FirSymbolProvider] provider for Kotlin/Native forward declarations in the module.
 *
 * @return a new symbol provider or `null` if the module of the passed [session] cannot contain forward declarations
 */
fun createNativeForwardDeclarationsSymbolProvider(session: LLFirSession): FirSymbolProvider? {
    val ktModule = session.ktModule
    val project = ktModule.project
    val packageProvider = project.createForwardDeclarationsPackageProvider(ktModule)
    val declarationProvider = project.createForwardDeclarationProvider(ktModule)

    check((packageProvider == null) == (declarationProvider == null)) {
        "Inconsistency between package and declaration providers for forward declarations. Both should be either null or non-null," +
                " but found: packageProvider $packageProvider; declarationProvider $declarationProvider"
    }
    if (packageProvider == null || declarationProvider == null) return null

    return LLFirNativeForwardDeclarationsSymbolProvider(
        session, declarationProvider, packageProvider,
    )
}
