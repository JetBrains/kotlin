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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrLiteralExpression<out T> : IrExpression {
    val kind: IrLiteralKind<T>
    val value: T
}

sealed class IrLiteralKind<out T>(val asString: kotlin.String)  {
    @Suppress("UNCHECKED_CAST")
    fun valueOf(literal: IrLiteralExpression<*>) =
            (literal as IrLiteralExpression<T>).value

    object Null : IrLiteralKind<Nothing?>("Null")
    object Boolean : IrLiteralKind<kotlin.Boolean>("Boolean")
    object Byte : IrLiteralKind<kotlin.Byte>("Byte")
    object Short : IrLiteralKind<kotlin.Short>("Short")
    object Int : IrLiteralKind<kotlin.Int>("Int")
    object Long : IrLiteralKind<kotlin.Long>("Long")
    object String : IrLiteralKind<kotlin.String>("String")
    object Float : IrLiteralKind<kotlin.Float>("Float")
    object Double : IrLiteralKind<kotlin.Double>("Double")

    override fun toString() = asString
}

class IrLiteralExpressionImpl<out T> (
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val kind: IrLiteralKind<T>,
        override val value: T
) : IrExpressionBase(startOffset, endOffset, type), IrLiteralExpression<T> {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitLiteral(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children
    }

    companion object {
        fun string(startOffset: Int, endOffset: Int, type: KotlinType, value: String): IrLiteralExpressionImpl<String> =
                IrLiteralExpressionImpl(startOffset, endOffset, type, IrLiteralKind.String, value)

        fun int(startOffset: Int, endOffset: Int, type: KotlinType, value: Int): IrLiteralExpressionImpl<Int> =
                IrLiteralExpressionImpl(startOffset, endOffset, type, IrLiteralKind.Int, value)
    }
}

