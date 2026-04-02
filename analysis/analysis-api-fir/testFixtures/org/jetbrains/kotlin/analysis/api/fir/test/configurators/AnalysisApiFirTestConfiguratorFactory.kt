/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.configurators

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

object AnalysisApiFirTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)

        val targetPlatform = data.targetPlatform.targetPlatform
        return when (data.moduleKind) {
            TestModuleKind.SourceLike -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> LLSourceLikeTestConfigurator(analyseInDependentSession = false, targetPlatform)
                AnalysisSessionMode.Dependent -> LLSourceLikeTestConfigurator(analyseInDependentSession = true, targetPlatform)
            }

            TestModuleKind.LibraryBinary -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibraryBinaryTestConfigurator(targetPlatform)
            }

            TestModuleKind.LibraryBinaryDecompiled -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibraryBinaryDecompiledTestConfigurator(targetPlatform)
            }

            TestModuleKind.LibrarySource -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                AnalysisApiFirLibrarySourceTestConfigurator(targetPlatform)
            }

            TestModuleKind.CodeFragment -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> AnalysisApiFirCodeFragmentTestConfigurator(analyseInDependentSession = false, targetPlatform)
                AnalysisSessionMode.Dependent -> AnalysisApiFirCodeFragmentTestConfigurator(
                    analyseInDependentSession = true,
                    targetPlatform
                )
            }

            else -> unsupportedModeError(data)
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean = when {
        data.frontend != FrontendKind.Fir -> false
        data.analysisApiMode != AnalysisApiMode.Ide -> false
        else -> when (data.moduleKind) {
            TestModuleKind.SourceLike,
                -> true

            TestModuleKind.Source,
            TestModuleKind.ScriptSource,
                -> false

            TestModuleKind.LibraryBinary,
            TestModuleKind.LibraryBinaryDecompiled,
            TestModuleKind.LibrarySource,
            TestModuleKind.CodeFragment,
                -> data.analysisSessionMode == AnalysisSessionMode.Normal

            TestModuleKind.NotUnderContentRoot,
            TestModuleKind.NotUnderContentRootWithDependencies,
                -> false
        }
    }
}
