/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.ModuleTransformerForSwitchingBackend

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractJvmIrAgainstOldBoxTestBase(targetBackend: TargetBackend) : AbstractKotlinCompilerWithTargetBackendTest(
    targetBackend
) {
    abstract val backendForLib: TargetBackend
    abstract val backendForMain: TargetBackend

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForCodegenTest(
            FrontendKinds.ClassicFrontend,
            ::ClassicFrontendFacade,
            ::ClassicFrontend2ClassicBackendConverter,
            ::ClassicJvmBackendFacade
        )

        commonHandlersForBoxTest()

        useFrontend2BackendConverters(::ClassicFrontend2IrConverter)
        useBackendFacades(::JvmIrBackendFacade)

        useModuleStructureTransformers(
            ModuleTransformerForSwitchingBackend(backendForLib, backendForMain)
        )

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(IGNORE_BACKEND_MULTI_MODULE))
    }
}


open class AbstractJvmIrAgainstOldBoxTest : AbstractJvmIrAgainstOldBoxTestBase(TargetBackend.JVM_MULTI_MODULE_IR_AGAINST_OLD) {
    override val backendForLib: TargetBackend
        get() = TargetBackend.JVM
    override val backendForMain: TargetBackend
        get() = TargetBackend.JVM_IR
}

open class AbstractJvmOldAgainstIrBoxTest : AbstractJvmIrAgainstOldBoxTestBase(TargetBackend.JVM_MULTI_MODULE_OLD_AGAINST_IR) {
    override val backendForLib: TargetBackend
        get() = TargetBackend.JVM_IR
    override val backendForMain: TargetBackend
        get() = TargetBackend.JVM
}
