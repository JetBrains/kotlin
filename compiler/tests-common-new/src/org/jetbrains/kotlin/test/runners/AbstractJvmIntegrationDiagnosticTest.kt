/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.cli.*
import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.FirTestDataConsistencyHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest

/**
 * In contrast to the normal diagnostic tests, compiles the modules in the test to JVM artifacts, and checks diagnostics along the way
 * (reported both by frontend and JVM backend).
 *
 * Does NOT run box, and at the moment does not include any backend-specific checkers like bytecode text, bytecode listing, etc.
 *
 * The main use case of this test is to check compilation errors/warnings in a leaf module in a complex module structure.
 * Note that if an error is reported in an intermediate module, no artifact is produced and the subsequent modules are not analyzed.
 */
abstract class AbstractJvmIntegrationDiagnosticTest(
    val targetFrontend: FrontendKind<*>,
) : AbstractKotlinCompilerTest() {
    abstract val jvmCliFacade: Constructor<JvmCliFacade>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }
        useDirectives(JvmEnvironmentConfigurationDirectives)

        facadeStep(jvmCliFacade)

        handlersStep(CliArtifact.Kind) {
            useHandlers(
                ::CliMetaInfoHandler,
                ::CliOutputHandler,
            )
        }

        defaultDirectives {
            +CHECK_COMPILER_OUTPUT
        }

        if (targetFrontend == FrontendKinds.ClassicFrontend) {
            useAfterAnalysisCheckers(::FirTestDataConsistencyHandler)
        } else {
            configurationForClassicAndFirTestsAlongside()
        }

        enableMetaInfoHandler()
    }
}

open class AbstractClassicJvmIntegrationDiagnosticTest : AbstractJvmIntegrationDiagnosticTest(FrontendKinds.ClassicFrontend) {
    override val jvmCliFacade: Constructor<JvmCliFacade> get() = ::ClassicJvmCliFacade
}

abstract class AbstractFirJvmIntegrationDiagnosticTest : AbstractJvmIntegrationDiagnosticTest(FrontendKinds.FIR)

open class AbstractFirLightTreeJvmIntegrationDiagnosticTest : AbstractFirJvmIntegrationDiagnosticTest() {
    override val jvmCliFacade: Constructor<JvmCliFacade> get() = ::FirLightTreeJvmCliFacade
}

@FirPsiCodegenTest
open class AbstractFirPsiJvmIntegrationDiagnosticTest : AbstractFirJvmIntegrationDiagnosticTest() {
    override val jvmCliFacade: Constructor<JvmCliFacade> get() = ::FirPsiJvmCliFacade
}
