/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary.LibraryAnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.source.LibrarySourceAnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.*

object AnalysisApiFirTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        require(supportMode(data))

        return when (data.moduleKind) {
            TestModuleKind.Source -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFirTestConfigurator(analyseInDependentSession = false)
                AnalysisSessionMode.Dependent -> AnalysisApiFirTestConfigurator(analyseInDependentSession = true)
            }

            TestModuleKind.LibraryBinary -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                LibraryAnalysisApiTestConfigurator
            }

            TestModuleKind.LibrarySource -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                LibrarySourceAnalysisApiTestConfigurator
            }
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        return when {
            data.frontend != FrontendKind.Fir -> false
            data.analysisApiMode != AnalysisApiMode.Ide -> false
            else -> when (data.moduleKind) {
                TestModuleKind.Source -> true
                TestModuleKind.LibraryBinary,
                TestModuleKind.LibrarySource ->
                    data.analysisSessionMode == AnalysisSessionMode.Normal
            }
        }
    }
}