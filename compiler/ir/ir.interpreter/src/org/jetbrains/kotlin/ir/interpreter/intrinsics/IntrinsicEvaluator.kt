/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.interpreter.ExecutionResult
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterMethodNotFoundError
import org.jetbrains.kotlin.ir.interpreter.stack.Stack
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal class IntrinsicEvaluator {
    fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        return when {
            EmptyArray.equalTo(irFunction) -> EmptyArray.evaluate(irFunction, stack, interpret)
            ArrayOf.equalTo(irFunction) -> ArrayOf.evaluate(irFunction, stack, interpret)
            ArrayOfNulls.equalTo(irFunction) -> ArrayOfNulls.evaluate(irFunction, stack, interpret)
            EnumValues.equalTo(irFunction) -> EnumValues.evaluate(irFunction, stack, interpret)
            EnumValueOf.equalTo(irFunction) -> EnumValueOf.evaluate(irFunction, stack, interpret)
            EnumHashCode.equalTo(irFunction) -> EnumHashCode.evaluate(irFunction, stack, interpret)
            JsPrimitives.equalTo(irFunction) -> JsPrimitives.evaluate(irFunction, stack, interpret)
            ArrayConstructor.equalTo(irFunction) -> ArrayConstructor.evaluate(irFunction, stack, interpret)
            SourceLocation.equalTo(irFunction) -> SourceLocation.evaluate(irFunction, stack, interpret)
            AssertIntrinsic.equalTo(irFunction) -> AssertIntrinsic.evaluate(irFunction, stack, interpret)
            else -> throw InterpreterMethodNotFoundError("Method ${irFunction.name} hasn't implemented")
        }
    }
}