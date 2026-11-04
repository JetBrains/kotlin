/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SOURCE_RANGES_IR
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest

abstract class AbstractJvmIrSourceRangesTest(parser: FirParser) : AbstractJvmIrTextTest(parser) {
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

open class AbstractFirLightTreeJvmIrSourceRangesTest : AbstractJvmIrSourceRangesTest(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiJvmIrSourceRangesTest : AbstractJvmIrSourceRangesTest(FirParser.Psi)
