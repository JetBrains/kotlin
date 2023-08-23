/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

object AnalysisApiFe10TestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        require(supportMode(data))

        return when (data.moduleKind) {
            TestModuleKind.Source -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFe10TestConfigurator
                AnalysisSessionMode.Dependent -> error("Unsupported AnalysisSessionMode.Dependent for fe10")
            }

            else -> {
                error("Unsupported non-source module for fe10")
            }
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        return when {
            data.frontend != FrontendKind.Fe10 -> false
            data.analysisSessionMode != AnalysisSessionMode.Normal -> false
            data.analysisApiMode != AnalysisApiMode.Ide -> false
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