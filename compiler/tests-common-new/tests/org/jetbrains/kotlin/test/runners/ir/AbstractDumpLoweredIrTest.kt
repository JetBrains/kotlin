/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.backend.common.lower.constEvaluationPhase
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.PhasedIrDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

open class AbstractDumpLoweredIrTest: AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = ArtifactKinds.Jvm
            targetBackend = TargetBackend.JVM_IR
            dependencyKind = DependencyKind.Source
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator
        )

        forTestsMatching("compiler/testData/ir/loweredIr/interpreter/*") {
            defaultDirectives {
                DUMP_IR_FOR_GIVEN_PHASES with constEvaluationPhase
            }
        }

        classicFrontendStep()
        psi2IrStep()
        jvmIrBackendStep()
        jvmArtifactsHandlersStep {
            useHandlers(::PhasedIrDumpHandler)
        }
    }
}