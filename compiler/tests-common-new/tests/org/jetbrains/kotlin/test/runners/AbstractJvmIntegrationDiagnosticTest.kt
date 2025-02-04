/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.cli.*
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds

/**
 * In contrast to the normal diagnostic tests, compiles the modules in the test to JVM artifacts, and checks diagnostics along the way
 * (reported both by frontend and JVM backend).
 *
 * Does NOT run box, and at the moment does not include any backend-specific checkers like bytecode text, bytecode listing, etc.
 *
 * The main use case of this test is to check compilation errors/warnings in a leaf module in a complex module structure.
 * Note that if an error is reported in an intermediate module, no artifact is produced and the subsequent modules are not analyzed.
 */
@OptIn(DeprecatedCliFacades::class)
open class AbstractClassicJvmIntegrationDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }
        useDirectives(JvmEnvironmentConfigurationDirectives)

        facadeStep(::ClassicJvmCliFacade)

        handlersStep(CliArtifact.Kind) {
            useHandlers(
                ::CliMetaInfoHandler,
                ::CliOutputHandler,
            )
        }

        defaultDirectives {
            +CHECK_COMPILER_OUTPUT
        }

        useAfterAnalysisCheckers(::FirTestDataConsistencyHandler)
        enableMetaInfoHandler()
    }
}
