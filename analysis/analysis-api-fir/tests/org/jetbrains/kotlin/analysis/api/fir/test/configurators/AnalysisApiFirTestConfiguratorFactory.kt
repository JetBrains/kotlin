/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.configurators

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirCodeFragmentTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibrarySourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

object AnalysisApiFirTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        require(supportMode(data))

        return when (data.moduleKind) {
            TestModuleKind.ScriptSource -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
                AnalysisSessionMode.Dependent -> AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = true)
            }

            TestModuleKind.Source -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
                AnalysisSessionMode.Dependent -> AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = true)
            }

            TestModuleKind.LibraryBinary -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibraryBinaryTestConfigurator
            }

            TestModuleKind.LibraryBinaryDecompiled -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
            }

            TestModuleKind.LibrarySource -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibrarySourceTestConfigurator
            }

            TestModuleKind.CodeFragment -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFirCodeFragmentTestConfigurator(analyseInDependentSession = false)
                AnalysisSessionMode.Dependent -> AnalysisApiFirCodeFragmentTestConfigurator(analyseInDependentSession = true)
            }

            else -> unsupportedModeError(data)
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        return when {
            data.frontend != FrontendKind.Fir -> false
            data.analysisApiMode != AnalysisApiMode.Ide -> false
            else -> when (data.moduleKind) {
                TestModuleKind.Source,
                TestModuleKind.ScriptSource -> {
                    true
                }

                TestModuleKind.LibraryBinary,
                TestModuleKind.LibraryBinaryDecompiled,
                TestModuleKind.LibrarySource,
                TestModuleKind.CodeFragment -> {
                    data.analysisSessionMode == AnalysisSessionMode.Normal
                }

                TestModuleKind.NotUnderContentRoot -> false
            }
        }
    }
}
