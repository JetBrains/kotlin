/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

object AnalysisApiFirStandaloneModeTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)

        return when (data.moduleKind) {
            TestModuleKind.Source -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> StandaloneModeConfigurator
                AnalysisSessionMode.Dependent -> unsupportedModeError(data)
            }

            else -> {
                unsupportedModeError(data)
            }
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        return when {
            data.frontend != FrontendKind.Fir -> false
            data.analysisSessionMode != AnalysisSessionMode.Normal -> false
            data.analysisApiMode != AnalysisApiMode.Standalone -> false
            else -> when (data.moduleKind) {
                TestModuleKind.Source -> true
                TestModuleKind.ScriptSource,
                TestModuleKind.LibraryBinary,
                TestModuleKind.LibrarySource,
                -> false
            }
        }
    }
}