/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined

import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.AbstractSymbolProviderTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

/**
 * @see LLCombinedPackageDelegationSymbolProvider
 */
abstract class AbstractCombinedPackageDelegationSymbolProviderTest : AbstractSymbolProviderTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun findTestSymbolProvider(mainModule: KtTestModule): FirSymbolProvider {
        val symbolProviders = mainModule.ktModule.findSymbolProvidersOfType<LLCombinedPackageDelegationSymbolProvider>()

        return symbolProviders.singleOrNull()
            ?: error("Expected a single `${LLCombinedPackageDelegationSymbolProvider::class.simpleName}` (candidates: $symbolProviders)")
    }
}
