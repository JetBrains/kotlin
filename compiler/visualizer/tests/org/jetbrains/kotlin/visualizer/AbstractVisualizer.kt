/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendOutputHandler
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractVisualizer : AbstractKotlinCompilerTest() {
    abstract val handler: Constructor<FrontendOutputHandler<*>>
    abstract val frontendKind: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<*>>

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
        useFrontendFacades(frontendFacade)
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

internal object VisualizerDirectives : SimpleDirectivesContainer() {
    val TEST_FILE_PATH by stringDirective(
        description = "Specify that part of test file path must be replaced with EXPECTED_FILE_PATH"
    )
    val EXPECTED_FILE_PATH by stringDirective(
        description = "Specify the path to expected result file that will be inserted instead of TEST_FILE_PATH"
    )
}