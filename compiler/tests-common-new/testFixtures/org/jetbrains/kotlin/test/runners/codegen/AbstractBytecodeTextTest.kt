/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractBytecodeTextTestBase(
    val parser: FirParser
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +WITH_STDLIB
            +WITH_REFLECT
        }

        setupJvmPipelineSteps(parser)
        commonHandlersForCodegenTest()

        configureJvmArtifactsHandlersStep {
            useHandlers(::BytecodeTextHandler)
        }

        useFailureSuppressors(::BlackBoxCodegenSuppressor)
    }
}


open class AbstractFirLightTreeBytecodeTextTest : AbstractBytecodeTextTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBytecodeTextTest : AbstractBytecodeTextTestBase(FirParser.Psi)
