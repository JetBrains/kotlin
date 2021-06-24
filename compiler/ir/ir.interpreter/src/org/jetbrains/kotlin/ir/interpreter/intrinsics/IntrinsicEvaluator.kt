/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.fqName

internal object IntrinsicEvaluator {
    fun unwindInstructions(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction>? {
        val fqName = irFunction.fqName
        return when {
            EmptyArray.canHandleFunctionWithName(fqName, irFunction.origin) -> EmptyArray.unwind(irFunction, environment)
            ArrayOf.canHandleFunctionWithName(fqName, irFunction.origin) -> ArrayOf.unwind(irFunction, environment)
            ArrayOfNulls.canHandleFunctionWithName(fqName, irFunction.origin) -> ArrayOfNulls.unwind(irFunction, environment)
            EnumValues.canHandleFunctionWithName(fqName, irFunction.origin) -> EnumValues.unwind(irFunction, environment)
            EnumValueOf.canHandleFunctionWithName(fqName, irFunction.origin) -> EnumValueOf.unwind(irFunction, environment)
            EnumIntrinsics.canHandleFunctionWithName(fqName, irFunction.origin) -> EnumIntrinsics.unwind(irFunction, environment)
            JsPrimitives.canHandleFunctionWithName(fqName, irFunction.origin) -> JsPrimitives.unwind(irFunction, environment)
            ArrayConstructor.canHandleFunctionWithName(fqName, irFunction.origin) -> ArrayConstructor.unwind(irFunction, environment)
            SourceLocation.canHandleFunctionWithName(fqName, irFunction.origin) -> SourceLocation.unwind(irFunction, environment)
            AssertIntrinsic.canHandleFunctionWithName(fqName, irFunction.origin) -> AssertIntrinsic.unwind(irFunction, environment)
            DataClassArrayToString.canHandleFunctionWithName(fqName, irFunction.origin) -> DataClassArrayToString.unwind(irFunction, environment)
            else -> null
        }
    }
}