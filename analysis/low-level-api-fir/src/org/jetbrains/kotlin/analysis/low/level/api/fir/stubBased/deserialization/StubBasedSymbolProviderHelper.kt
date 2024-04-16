/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider

class StubBasedSymbolProviderHelper(
    project: Project,
    scope: GlobalSearchScope,
    session: FirSession,
    private val isFallbackDependenciesProvider: Boolean,
) {
    val declarationProvider = project.createDeclarationProvider(
        scope,
        contextualModule = session.llFirModuleData.ktModule.takeIf { !isFallbackDependenciesProvider },
    )

    val packageProvider: KotlinPackageProvider = project.createPackageProvider(scope)

    val symbolNamesProvider: FirSymbolNamesProvider = LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider)
}
