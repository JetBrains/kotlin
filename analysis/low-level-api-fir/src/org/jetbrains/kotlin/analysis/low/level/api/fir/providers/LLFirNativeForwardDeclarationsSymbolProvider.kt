/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createForwardDeclarationsPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createForwardDeclarationProvider
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
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
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    override val declarationProvider: KotlinDeclarationProvider,
    override val packageProvider: KotlinPackageProvider,
) : LLFirKotlinSymbolProvider(
    session,
) {
    private val moduleData: FirModuleData = moduleDataProvider.getModuleData(path = null)

    /**
     * Forward declarations are not defined in `kotlin` package
     */
    override val allowKotlinPackage: Boolean get() = false

    override val symbolNamesProvider: FirSymbolNamesProvider =
        LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider, allowKotlinPackage)

    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, KtClassLikeDeclaration> =
        session.firCachesFactory.createCache(
            createValue = { classId: ClassId, declaration: KtClassLikeDeclaration ->
                createSyntheticForwardDeclarationClass(classId, moduleData, this.session, kotlinScopeProvider) {
                    source = KtRealPsiSourceElement(declaration)
                }
            }
        )

    override fun getPackage(fqName: FqName): FqName? = fqName.takeIf { packageProvider.doesKotlinOnlyPackageExist(fqName) }

    @FirSymbolProviderInternals
    override fun getClassLikeSymbolByClassId(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): FirClassLikeSymbol<*>? {
        return classCache.getValue(classId, classLikeDeclaration)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val declaration = declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null
        @OptIn(FirSymbolProviderInternals::class)
        return getClassLikeSymbolByClassId(classId, declaration)
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
fun createNativeForwardDeclarationsSymbolProvider(
    project: Project,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider? {
    val ktModule = session.llFirModuleData.ktModule
    val packageProvider = project.createForwardDeclarationsPackageProvider(ktModule)
    val declarationProvider = project.createForwardDeclarationProvider(ktModule)

    check((packageProvider == null) == (declarationProvider == null)) {
        "Inconsistency between package and declaration providers for forward declarations. Both should be either null or non-null," +
                " but found: packageProvider $packageProvider; declarationProvider $declarationProvider"
    }
    if (packageProvider == null || declarationProvider == null) return null

    return LLFirNativeForwardDeclarationsSymbolProvider(
        session, moduleDataProvider, kotlinScopeProvider, declarationProvider, packageProvider,
    )
}
