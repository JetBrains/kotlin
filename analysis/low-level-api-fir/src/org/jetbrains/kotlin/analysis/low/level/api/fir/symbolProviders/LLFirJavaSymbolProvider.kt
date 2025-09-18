/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaDeclarationNamesProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsOnlyApi
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDelegatingCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProviderWithoutCallables
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

internal class LLFirJavaSymbolProvider private constructor(
    session: LLFirSession,
    javaFacade: FirJavaFacade,
    val searchScope: GlobalSearchScope
) : JavaSymbolProvider(session, javaFacade), LLPsiAwareSymbolProvider {
    constructor(session: LLFirSession, searchScope: GlobalSearchScope) : this(
        session,
        FirJavaFacadeForSource(
            session,
            session.moduleData,
            session.project.createJavaClassFinder(searchScope)
        ),
        searchScope
    )

    private val psiAwareCache = LLPsiAwareClassLikeSymbolCache<PsiClass, FirRegularClassSymbol?, ClassCacheContext?>(
        classCache,
        session.firCachesFactory.createCache { psiClass, classCacheContext ->
            javaFacade.createPsiClassSymbol(psiClass, classCacheContext?.foundJavaClass, classCacheContext?.parentClassSymbol)
        }
    )

    // TODO: Hacky!
    override val symbolNamesProvider: FirSymbolNamesProvider = FirDelegatingCachedSymbolNamesProvider(
        session,
        object : FirSymbolNamesProviderWithoutCallables() {
            private val javaDeclarationNamesProvider = KotlinJavaDeclarationNamesProvider.getInstance(session.project)

            override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false

            override fun getPackageNames(): Set<String>? = javaDeclarationNamesProvider?.computePackageNames(session.ktModule)

            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
                javaFacade.knownClassNamesInPackage(packageFqName)?.mapToSetOrEmpty { Name.identifier(it) }
        },
    )

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return getClassLikeSymbolByClassId(classId, null)
    }

    /**
     * [KotlinJavaDeclarationNamesProvider] provides *exact* package sets (including parent packages), so we can decide [hasPackage] without
     * querying the Java facade.
     */
    override fun hasPackage(fqName: FqName): Boolean {
        val packageNames = symbolNamesProvider.getPackageNames()
            ?: return super.hasPackage(fqName)

        return fqName.asString() in packageNames
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirRegularClassSymbol? =
        psiAwareCache.getSymbolByPsi<PsiClass>(classId, declaration) { psiClass ->
            val parentClass = getParentPsiClassSymbol(psiClass)
            ClassCacheContext(parentClass, JavaClassImpl(psiClass))
        }

    @LLStatisticsOnlyApi
    internal val cachedDeclarations: Collection<FirDeclaration>
        get() = psiAwareCache.cachedValues.mapNotNull { it?.fir }
}

internal val FirSession.nullableJavaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()
