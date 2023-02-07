/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

interface JsStatementOrigins {
    object BIND_CALL : IrStatementOriginImpl("BIND_CALL")
    object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    object SYNTHESIZED_STATEMENT : IrStatementOriginImpl("SYNTHESIZED_STATEMENT")
    object CALLABLE_REFERENCE_CREATE : IrStatementOriginImpl("CALLABLE_REFERENCE_CREATE")
    object CALLABLE_REFERENCE_INVOKE : IrStatementOriginImpl("CALLABLE_REFERENCE_INVOKE")
    object EXPLICIT_INVOKE : IrStatementOriginImpl("EXPLICIT_INVOKE")
    object FACTORY_ORIGIN : IrDeclarationOriginImpl("FACTORY_ORIGIN")
    object COROUTINE_ROOT_LOOP : IrStatementOriginImpl("COROUTINE_ROOT_LOOP")
    object COROUTINE_SWITCH : IrStatementOriginImpl("COROUTINE_SWITCH")
    object CLASS_REFERENCE : IrStatementOriginImpl("CLASS_REFERENCE")
    object IMPLEMENTATION_DELEGATION_CALL : IrStatementOriginImpl("IMPLEMENTATION_DELEGATION_CALL")
}