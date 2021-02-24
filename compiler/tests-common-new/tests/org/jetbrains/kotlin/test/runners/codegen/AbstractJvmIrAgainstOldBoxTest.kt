/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.ModuleTransformerForSwitchingBackend
import org.jetbrains.kotlin.test.services.ModuleTransformerForTwoFilesBoxTests

@OptIn(TestInfrastructureInternals::class)
open class AbstractBackendAgainstBackendBoxTestBase(
    targetBackend: TargetBackend,
    val backendForLib: TargetBackend,
    val backendForMain: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForCodegenTest(
            FrontendKinds.ClassicFrontend,
            ::ClassicFrontendFacade,
            ::ClassicFrontend2ClassicBackendConverter,
            ::ClassicJvmBackendFacade
        )
        useFrontend2BackendConverters(::ClassicFrontend2IrConverter)
        useBackendFacades(::JvmIrBackendFacade)

        commonHandlersForBoxTest()
        useInlineHandlers()

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE))

        useModuleStructureTransformers(
            ModuleTransformerForTwoFilesBoxTests(),
            ModuleTransformerForSwitchingBackend(backendForLib, backendForMain)
        )
    }
}

open class AbstractJvmIrAgainstOldBoxInlineTest : AbstractBackendAgainstBackendBoxTestBase(
    targetBackend = TargetBackend.JVM_MULTI_MODULE_IR_AGAINST_OLD,
    backendForLib = TargetBackend.JVM,
    backendForMain = TargetBackend.JVM_IR
)

open class AbstractJvmOldAgainstIrBoxInlineTest : AbstractBackendAgainstBackendBoxTestBase(
    targetBackend = TargetBackend.JVM_MULTI_MODULE_OLD_AGAINST_IR,
    backendForLib = TargetBackend.JVM_IR,
    backendForMain = TargetBackend.JVM_OLD
)
