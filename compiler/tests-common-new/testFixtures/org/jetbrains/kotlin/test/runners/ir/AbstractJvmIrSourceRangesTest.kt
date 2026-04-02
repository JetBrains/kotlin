/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SOURCE_RANGES_IR
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.ResultingArtifact

abstract class AbstractJvmIrSourceRangesTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    targetFrontend: FrontendKind<FrontendOutput>
) : AbstractJvmIrTextTest<FrontendOutput>(targetFrontend) {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DUMP_SOURCE_RANGES_IR
                -DUMP_KT_IR
                -DUMP_IR
            }
        }
    }
}
