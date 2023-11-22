/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.ModuleTransformerForSwitchingBackend
import org.jetbrains.kotlin.utils.bind

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractBoxWithDifferentBackendsTest(
    targetBackend: TargetBackend,
    val backendForLib: TargetBackend,
    val backendForMain: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    override fun TestConfigurationBuilder.configuration() {
        commonServicesConfigurationForCodegenTest(FrontendKinds.ClassicFrontend)

        classicFrontendStep()
        classicFrontendHandlersStep {
            commonClassicFrontendHandlersForCodegenTest()
        }

        psi2ClassicBackendStep()
        classicJvmBackendStep()

        psi2IrStep()
        jvmIrBackendStep()

        jvmArtifactsHandlersStep {
            commonBackendHandlersForCodegenTest()
            boxHandlersForBackendStep()
        }

        useModuleStructureTransformers(
            ModuleTransformerForSwitchingBackend(backendForLib, backendForMain)
        )

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(IGNORE_BACKEND_MULTI_MODULE))
    }
}

open class AbstractJvmIrAgainstOldBoxTest : AbstractBoxWithDifferentBackendsTest(
    TargetBackend.JVM_MULTI_MODULE_IR_AGAINST_OLD,
    backendForLib = TargetBackend.JVM,
    backendForMain = TargetBackend.JVM_IR,
)

open class AbstractJvmOldAgainstIrBoxTest : AbstractBoxWithDifferentBackendsTest(
    TargetBackend.JVM_MULTI_MODULE_OLD_AGAINST_IR,
    backendForLib = TargetBackend.JVM_IR,
    backendForMain = TargetBackend.JVM,
)
