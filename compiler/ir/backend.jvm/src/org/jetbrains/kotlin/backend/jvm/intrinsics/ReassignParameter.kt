/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol

// A magic intrinsic that changes the supposedly-immutable parameter referenced by its argument:
//
//     fun f(x: Int) {
//         <set-parameter>(x, 1)
//         return x
//     }
//     assert(f(2) == 1)
//
// This *only* works for parameters; any other variable should be made mutable instead.
//
// This is used to optimize default method stubs (and, more importantly, produce bytecode that
// the inliner can actually handle) in `JvmDefaultArgumentStubGenerator`.
object ReassignParameter : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val parameterGet = expression.getValueArgument(0) as? IrGetValue
        val parameter = parameterGet?.symbol as? IrValueParameterSymbol
            ?: throw AssertionError("${expression.getValueArgument(0)} is not a get of a parameter")
        codegen.setVariable(parameter, expression.getValueArgument(1)!!, data)
        return codegen.immaterialUnitValue
    }
}
