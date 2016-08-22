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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullabilityFlexible

interface IrBodyGenerator : IrGenerator {
    val scope: Scope
}

abstract class IrChildBodyGeneratorBase<out T : IrBodyGenerator>(val parentGenerator: T) : IrBodyGenerator {
    override val context: GeneratorContext get() = parentGenerator.context
    override val scope: Scope get() = parentGenerator.scope
}

fun IrBodyGenerator.toExpectedType(irExpression: IrExpression, expectedType: KotlinType?): IrExpression {
    if (irExpression is IrBlock && !irExpression.hasResult) return irExpression

    if (expectedType == null) return irExpression
    if (KotlinBuiltIns.isUnit(expectedType)) return irExpression // TODO expose coercion to Unit in IR?

    val valueType = irExpression.type ?: throw AssertionError("expectedType != null, valueType == null: $this")

    if (valueType.isNullabilityFlexible() && !expectedType.isMarkedNullable) {
        return IrUnaryOperatorImpl(irExpression.startOffset, irExpression.endOffset, IrOperator.IMPLICIT_NOTNULL,
                                   context.irBuiltIns.implicitNotNull,
                                   irExpression)
    }

    if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
        return IrTypeOperatorCallImpl(irExpression.startOffset, irExpression.endOffset, expectedType,
                                      IrTypeOperator.IMPLICIT_CAST, expectedType, irExpression)
    }

    return irExpression
}

fun StatementGenerator.generateExpressionWithExpectedType(ktExpression: KtExpression, expectedType: KotlinType?) =
        toExpectedType(generateExpression(ktExpression), expectedType)

fun IrBodyGenerator.toExpectedTypeOrNull(irExpression: IrExpression?, expectedType: KotlinType?): IrExpression? =
        irExpression?.let { toExpectedType(it, expectedType) }