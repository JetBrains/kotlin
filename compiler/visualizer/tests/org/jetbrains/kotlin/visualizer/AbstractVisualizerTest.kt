/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendOutputHandler
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractVisualizerTest : AbstractKotlinCompilerTest() {
    abstract val handler: Constructor<FrontendOutputHandler<*>>
    abstract val frontendKind: FrontendKind<*>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = frontendKind
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )
        useFrontendFacades(
            ::FirFrontendFacade,
            ::ClassicFrontendFacade
        )
        useFrontendHandlers(handler)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.WITH_STDLIB
        }

        forTestsMatching("compiler/fir/raw-fir/psi2fir/testData/rawBuilder/declarations/*") {
            defaultDirectives {
                VisualizerDirectives.TEST_FILE_PATH with "fir/raw-fir/psi2fir"
                VisualizerDirectives.EXPECTED_FILE_PATH with "visualizer"
            }
        }

        forTestsMatching("compiler/fir/raw-fir/psi2fir/testData/rawBuilder/expressions/*") {
            defaultDirectives {
                VisualizerDirectives.TEST_FILE_PATH with "fir/raw-fir/psi2fir"
                VisualizerDirectives.EXPECTED_FILE_PATH with "visualizer"
            }
        }

        forTestsMatching("compiler/visualizer/testData/uncommonCases/*") {
            defaultDirectives {
                VisualizerDirectives.TEST_FILE_PATH with "uncommonCases/testFiles"
                VisualizerDirectives.EXPECTED_FILE_PATH with "uncommonCases/resultFiles"
            }
        }
    }
}

