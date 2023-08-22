/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

object JvmLoweredStatementOrigin {
    val DEFAULT_STUB_CALL_TO_IMPLEMENTATION by IrStatementOriginImpl
    val DO_WHILE_COUNTER_LOOP by IrStatementOriginImpl
    val INLINE_LAMBDA by IrStatementOriginImpl
    val FAKE_CONTINUATION by IrStatementOriginImpl

    val FOLDED_SAFE_CALL by IrStatementOriginImpl
    val FOLDED_ELVIS by IrStatementOriginImpl
}
