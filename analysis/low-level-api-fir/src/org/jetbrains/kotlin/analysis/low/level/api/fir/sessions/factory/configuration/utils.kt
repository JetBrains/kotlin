/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackagePartProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun createSymbolProvidersWithOptionalAnnotationClassesProvider(
    session: LLFirSession,
    scope: GlobalSearchScope,
    createSymbolProviders: (PackagePartProvider) -> List<FirSymbolProvider>,
): List<FirSymbolProvider> {
    val project = session.project
    val moduleDataProvider = SingleModuleDataProvider(session.moduleData)
    val packagePartProvider = project.createPackagePartProvider(scope)

    return buildList {
        addAll(createSymbolProviders(packagePartProvider))

        addIfNotNull(
            OptionalAnnotationClassesProvider.createIfNeeded(
                session,
                moduleDataProvider,
                session.kotlinScopeProvider,
                packagePartProvider,
            )
        )
    }
}

internal fun createCloneableSymbolProvider(session: LLFirBuiltinsAndCloneableSession): FirSymbolProvider {
    val cloneableProvider = FirCloneableSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
    return cloneableProvider
}
