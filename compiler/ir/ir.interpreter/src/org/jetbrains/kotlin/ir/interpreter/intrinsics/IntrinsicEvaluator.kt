/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterMethodNotFoundError

internal object IntrinsicEvaluator {
    fun unwindInstructions(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        return when {
            EmptyArray.equalTo(irFunction) -> EmptyArray.unwind(irFunction, environment)
            ArrayOf.equalTo(irFunction) -> ArrayOf.unwind(irFunction, environment)
            ArrayOfNulls.equalTo(irFunction) -> ArrayOfNulls.unwind(irFunction, environment)
            EnumValues.equalTo(irFunction) -> EnumValues.unwind(irFunction, environment)
            EnumValueOf.equalTo(irFunction) -> EnumValueOf.unwind(irFunction, environment)
            EnumHashCode.equalTo(irFunction) -> EnumHashCode.unwind(irFunction, environment)
            JsPrimitives.equalTo(irFunction) -> JsPrimitives.unwind(irFunction, environment)
            ArrayConstructor.equalTo(irFunction) -> ArrayConstructor.unwind(irFunction, environment)
            SourceLocation.equalTo(irFunction) -> SourceLocation.unwind(irFunction, environment)
            AssertIntrinsic.equalTo(irFunction) -> AssertIntrinsic.unwind(irFunction, environment)
            else -> throw InterpreterMethodNotFoundError("Method ${irFunction.name} hasn't implemented")
        }
    }
}