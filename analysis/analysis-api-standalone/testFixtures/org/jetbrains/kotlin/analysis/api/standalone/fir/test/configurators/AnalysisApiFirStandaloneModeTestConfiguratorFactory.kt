/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

object AnalysisApiFirStandaloneModeTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)
        val targetPlatform = data.targetPlatform.targetPlatform
        return when (data.moduleKind) {
            TestModuleKind.Source -> StandaloneModeConfigurator(targetPlatform)
            TestModuleKind.LibraryBinary -> StandaloneModeLibraryBinaryTestConfigurator(targetPlatform)
            TestModuleKind.LibraryBinaryDecompiled -> StandaloneModeLibraryBinaryDecompiledTestConfigurator(targetPlatform)
            else -> unsupportedModeError(data)
        }
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean = when {
        data.analysisSessionMode != AnalysisSessionMode.Normal -> false
        data.analysisApiMode != AnalysisApiMode.Standalone -> false
        else -> when (data.moduleKind) {
            TestModuleKind.Source,
            TestModuleKind.LibraryBinary,
            TestModuleKind.LibraryBinaryDecompiled,
            TestModuleKind.CodeFragment,
                -> true

            TestModuleKind.ScriptSource,
            TestModuleKind.SourceLike,
            TestModuleKind.LibrarySource,
            TestModuleKind.NotUnderContentRoot,
            TestModuleKind.NotUnderContentRootWithDependencies,
                -> false
        }
    }
}
