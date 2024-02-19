/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

object LoweredStatementOrigins {
    val STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE = IrStatementOriginImpl("INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE")
    val SYNTHESIZED_INIT_BLOCK by IrStatementOriginImpl
    val DEFAULT_DISPATCH_CALL by IrStatementOriginImpl

    val INLINED_FUNCTION_REFERENCE by IrStatementOriginImpl
    val INLINED_FUNCTION_ARGUMENTS by IrStatementOriginImpl
    val INLINED_FUNCTION_DEFAULT_ARGUMENTS by IrStatementOriginImpl

}
