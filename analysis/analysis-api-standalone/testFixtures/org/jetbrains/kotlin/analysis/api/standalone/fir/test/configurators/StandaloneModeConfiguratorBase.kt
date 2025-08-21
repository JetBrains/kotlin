/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class StandaloneModeConfiguratorBase : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val analysisApiMode: AnalysisApiMode get() = AnalysisApiMode.Standalone
    override val frontendKind: FrontendKind get() = FrontendKind.Fir
    override val testPrefixes: List<String> get() = listOf("standalone.fir")

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING) }
        }
    }
}
