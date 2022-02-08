/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

interface JvmLoweredStatementOrigin {
    object DEFAULT_STUB_CALL_TO_IMPLEMENTATION : IrStatementOriginImpl("DEFAULT_STUB_CALL_TO_IMPLEMENTATION")
    object DO_WHILE_COUNTER_LOOP: IrStatementOriginImpl("DO_WHILE_COUNTER_LOOP")
    object INLINE_LAMBDA : IrStatementOriginImpl("INLINE_LAMBDA")
    object FAKE_CONTINUATION : IrStatementOriginImpl("FAKE_CONTINUATION")

    object FOLDED_SAFE_CALL : IrStatementOriginImpl("FOLDED_SAFE_CALL")
    object FOLDED_ELVIS : IrStatementOriginImpl("FOLDED_ELVIS")
}
