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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullabilityFlexible

fun IrExpression.toExpectedType(expectedType: KotlinType?): IrExpression {
    if (expectedType == null) return this
    if (KotlinBuiltIns.isUnit(expectedType)) return this // TODO expose coercion to Unit in IR?

    val valueType = type ?: throw AssertionError("expectedType != null, valueType == null: $this")

    if (valueType.isNullabilityFlexible() && !expectedType.isMarkedNullable) {
        return IrUnaryOperatorExpressionImpl(startOffset, endOffset, expectedType,
                                             IrOperator.IMPLICIT_NOTNULL, null, this)
    }

    if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
        return IrTypeOperatorExpressionImpl(startOffset, endOffset, expectedType,
                                            IrTypeOperator.IMPLICIT_CAST, expectedType, this)
    }

    return this
}

fun IrVariable.load(): IrExpression =
        IrGetVariableExpressionImpl(startOffset, endOffset, descriptor)