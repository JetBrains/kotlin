/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories.LLLibrarySymbolProviderFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

internal class LLMetadataSessionConfiguration(private val project: Project) : LLPlatformSessionConfiguration {
    override fun createBinaryLibrarySymbolProviders(session: LLFirSession, scope: GlobalSearchScope): List<FirSymbolProvider> =
        createSymbolProvidersWithOptionalAnnotationClassesProvider(session, scope) { packagePartProvider ->
            LLLibrarySymbolProviderFactory.fromSettings(project).createMetadataLibrarySymbolProvider(
                session,
                packagePartProvider,
                scope,
            )
        }

    override fun createPlatformSpecificSymbolProvidersForBuiltinsSession(
        session: LLFirBuiltinsAndCloneableSession
    ): List<FirSymbolProvider> {
        /** Aligned with [org.jetbrains.kotlin.fir.session.FirMetadataSessionFactory] (adds `Cloneable` similarly to JVM). */
        return listOf(createCloneableSymbolProvider(session))
    }
}
