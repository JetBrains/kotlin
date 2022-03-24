/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary.LibraryAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.source.LibrarySourceAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.*

object AnalysisApiFirTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfiguratorService {
        require(supportMode(data))

        return when (data.moduleKind) {
            TestModuleKind.Source -> when (data.analysisSessionMode) {
                AnalysisSessionMode.Normal -> FirAnalysisApiNormalModeTestConfiguratorService
                AnalysisSessionMode.Dependent -> FirAnalysisApiDependentModeTestConfiguratorService
            }

            TestModuleKind.LibraryBinary -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                LibraryAnalysisApiTestConfiguratorService
            }

            TestModuleKind.LibrarySource -> {
                require(data.analysisSessionMode == AnalysisSessionMode.Normal)
                LibrarySourceAnalysisApiTestConfiguratorService
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