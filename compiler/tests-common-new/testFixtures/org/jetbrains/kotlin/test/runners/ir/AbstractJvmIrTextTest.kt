/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.JvmNewKotlinReflectCompatibilityCheck
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJvmIrTextTest(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        setupJvmPipelineSteps(parser)
        commonHandlersForCodegenTest()
        setupDefaultDirectivesForIrTextTest()
        configureIrHandlersStep {
            setupIrTextDumpHandlers()
        }
        additionalK2ConfigurationForIrTextTest(parser)

        useFailureSuppressors(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.BACKEND)
        )
        enableMetaInfoHandler()
    }
}


open class AbstractFirLightTreeJvmIrTextTest : AbstractJvmIrTextTest(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureJvmArtifactsHandlersStep {
                useHandlers(::JvmNewKotlinReflectCompatibilityCheck)
            }
        }
    }
}

@FirPsiCodegenTest
open class AbstractFirPsiJvmIrTextTest : AbstractJvmIrTextTest(FirParser.Psi)
