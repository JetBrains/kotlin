/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetVariableExpressionImpl
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorExpressionImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

fun IrExpression.toExpectedType(expectedType: KotlinType?): IrExpression {
    if (expectedType == null) return this
    val valueType = type ?: throw AssertionError("expectedType != null, valueType == null: $this")
    if (KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
        return this
    }

    return IrTypeOperatorExpressionImpl(
            startOffset, endOffset, expectedType,
            IrTypeOperator.IMPLICIT_CAST, expectedType,
            this
    )
}

fun IrVariable.createDefaultGetExpression(): IrExpression =
        IrGetVariableExpressionImpl(startOffset, endOffset, descriptor)