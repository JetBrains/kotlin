/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId

/**
 * [LLJvmClassFileBasedSymbolProvider] loads Kotlin and Java symbols from JVM class files. It is used in Standalone, while the IDE uses
 * [LLKotlinStubBasedLibrarySymbolProvider] instead.
 */
internal class LLJvmClassFileBasedSymbolProvider(
    session: LLFirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    packagePartProvider: PackagePartProvider,
    kotlinClassFinder: KotlinClassFinder,
    javaFacade: FirJavaFacade,
) : JvmClassFileBasedSymbolProvider(
    session,
    moduleDataProvider,
    kotlinScopeProvider,
    packagePartProvider,
    kotlinClassFinder,
    javaFacade
), LLPsiAwareSymbolProvider {
    /**
     * We don't use [LLPsiAwareClassLikeSymbolCache][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache]
     * here because we only need to cache Java ambiguities (see the comment below). This doesn't match up with the main cache: [classCache]
     * contains both Kotlin and Java classes, and has a different context type.
     */
    private val psiClassAmbiguityCache: FirCache<PsiClass, FirRegularClassSymbol, FirRegularClassSymbol?> =
        session.firCachesFactory.createCache { psiClass, parentClassSymbol ->
            javaFacade.createPsiClassSymbol(psiClass, JavaClassImpl(psiClass), parentClassSymbol)
        }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>? {
        getClassLikeSymbolByClassId(classId)
            ?.takeIf { it.hasPsi(declaration) }
            ?.let { return it }

        // `LLJvmClassFileBasedSymbolProvider` deserializes both Kotlin and Java class symbols, but only Java symbols have associated PSI
        // elements (`ClsClassImpl`). Hence, it is currently not possible to query `getClassLikeSymbolByPsi` with compiled Kotlin PSI
        // declarations. If or where such PSI declarations exist and whether they can be sourced without hacks is currently unclear. In any
        // case, Standalone would explicitly have to add support for such PSI declarations, and it'd require a strong use case.
        //
        // Compare KT-65836, where we'd add `STUBS` support for Standalone, associating library symbols with their PSI declarations.
        // However, after that issue is fixed, `LLKotlinStubBasedLibrarySymbolProvider` would be used instead, so this symbol provider
        // wouldn't have to support Kotlin PSI.
        //
        // In summary, `LLJvmClassFileBasedSymbolProvider` only has to deal with Java PSI right now.
        //
        // Even when use-site analysis of library modules is allowed in Standalone (KT-76042), this symbol provider likely won't be queried
        // by Kotlin PSI. The library module will likely be analyzed with decompiled sources, whose symbols will be provided by symbol
        // providers for sources, i.e. `LLFirJavaSymbolProvider` and `LLKotlinSourceSymbolProvider`.
        if (declaration is PsiClass) {
            val parentClassSymbol = getParentPsiClassSymbol(declaration)
            return psiClassAmbiguityCache.getValue(declaration, parentClassSymbol)
        }
        return null
    }
}
