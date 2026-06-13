/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirDanglingFileSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLFirJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories.LLLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.nullableJavaSymbolProvider
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.javaAnnotationProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass

internal class LLJvmSessionConfiguration(private val project: Project) : LLPlatformSessionConfiguration {
    override fun createSourceScopeProvider(): FirKotlinScopeProvider =
        FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

    override fun createBuiltinsScopeProvider(): FirKotlinScopeProvider =
        FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

    override fun createPlatformSpecificSymbolProviders(
        session: LLFirSession,
        contentScope: GlobalSearchScope,
    ): List<FirSymbolProvider> =
        listOf(LLFirJavaSymbolProvider(session, contentScope))

    override fun createPlatformSpecificSymbolProvidersForDanglingFileSession(
        session: LLFirDanglingFileSession,
        contextSession: LLFirSession,
    ): List<FirSymbolProvider> =
        listOfNotNull(contextSession.nullableJavaSymbolProvider)

    override fun createPlatformSpecificSymbolProvidersForBuiltinsSession(
        session: LLFirBuiltinsAndCloneableSession
    ): List<FirSymbolProvider> = listOf(createCloneableSymbolProvider(session))

    override fun createBinaryLibrarySymbolProviders(session: LLFirSession, scope: GlobalSearchScope): List<FirSymbolProvider> =
        createSymbolProvidersWithOptionalAnnotationClassesProvider(session, scope) { packagePartProvider ->
            val javaClassFinder = project.createJavaClassFinder(scope, session.javaAnnotationProvider)
            val firJavaFacade = LLFirJavaFacadeForBinaries(session, javaClassFinder)

            LLLibrarySymbolProviderFactory.fromSettings(project).createJvmLibrarySymbolProvider(
                session,
                firJavaFacade,
                packagePartProvider,
                scope,
            )
        }
}

private class LLFirJavaFacadeForBinaries(
    private val session: LLFirSession,
    classFinder: JavaClassFinder,
) : FirJavaFacade(session, classFinder) {
    override fun getModuleDataForClass(javaClass: JavaClass): FirModuleData = session.moduleData
}
