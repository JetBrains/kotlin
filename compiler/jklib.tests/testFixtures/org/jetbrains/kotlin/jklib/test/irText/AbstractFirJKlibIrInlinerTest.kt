/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_INLINE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR

/**
 * Checks the IR produced by the inliner of the JKlib first phase, which runs before the KLIB serialization.
 */
abstract class AbstractFirJKlibIrInlinerTest : AbstractFirJKlibIrTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            -DUMP_IR
            -DUMP_KT_IR
            +DUMP_IR_AFTER_INLINE
        }
    }
}
