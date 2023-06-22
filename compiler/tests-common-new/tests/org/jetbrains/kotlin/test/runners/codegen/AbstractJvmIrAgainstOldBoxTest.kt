/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.services.ModuleTransformerForSwitchingBackend
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractBoxInlineWithDifferentBackendsTest(
    targetBackend: TargetBackend,
    backendForLib: TargetBackend,
    backendForMain: TargetBackend
) : AbstractBoxWithDifferentBackendsTest(targetBackend, backendForLib, backendForMain) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.apply {
            applyDumpSmapDirective()

            resetModuleStructureTransformers()
            useModuleStructureTransformers(
                SplittingModuleTransformerForBoxTests(),
                ModuleTransformerForSwitchingBackend(backendForLib, backendForMain)
            )

            configureJvmArtifactsHandlersStep {
                inlineHandlers()
            }
        }
    }
}

open class AbstractJvmIrAgainstOldBoxInlineTest : AbstractBoxInlineWithDifferentBackendsTest(
    targetBackend = TargetBackend.JVM_MULTI_MODULE_IR_AGAINST_OLD,
    backendForLib = TargetBackend.JVM,
    backendForMain = TargetBackend.JVM_IR
)

open class AbstractJvmOldAgainstIrBoxInlineTest : AbstractBoxInlineWithDifferentBackendsTest(
    targetBackend = TargetBackend.JVM_MULTI_MODULE_OLD_AGAINST_IR,
    backendForLib = TargetBackend.JVM_IR,
    backendForMain = TargetBackend.JVM
)
