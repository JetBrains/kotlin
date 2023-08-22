/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

object JsStatementOrigins {
    val BIND_CALL by IrStatementOriginImpl
    val STATEMENT_ORIGIN_COROUTINE_IMPL = IrStatementOriginImpl("COROUTINE_IMPL")
    val SYNTHESIZED_STATEMENT by IrStatementOriginImpl
    val CALLABLE_REFERENCE_CREATE by IrStatementOriginImpl
    val CALLABLE_REFERENCE_INVOKE by IrStatementOriginImpl
    val EXPLICIT_INVOKE by IrStatementOriginImpl

    val FACTORY_ORIGIN by IrDeclarationOriginImpl

    val COROUTINE_ROOT_LOOP by IrStatementOriginImpl
    val COROUTINE_SWITCH by IrStatementOriginImpl
    val CLASS_REFERENCE by IrStatementOriginImpl
    val IMPLEMENTATION_DELEGATION_CALL by IrStatementOriginImpl
}