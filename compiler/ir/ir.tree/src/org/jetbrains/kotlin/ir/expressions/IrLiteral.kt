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

import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrLiteral : IrExpression

interface IrNullLiteral : IrLiteral

interface IrTrueLiteral : IrLiteral

interface IrFalseLiteral : IrLiteral

interface IrIntLiteral : IrLiteral {
    val value: Int
}

interface IrLongLiteral : IrLiteral {
    val value: Long
}

interface IrFloatLiteral : IrLiteral {
    val value: Float
}

interface IrDoubleLiteral : IrLiteral {
    val value: Double
}

interface IrCharLiteral : IrLiteral {
    val value: Char
}

interface IrStringLiteral : IrLiteral {
    val value: String
}

abstract class IrLiteralBase(
        sourceLocation: SourceLocation,
        type: KotlinType
) : IrExpressionBase(sourceLocation, type), IrLiteral {
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children
    }
}

class IrIntLiteralImpl(
        sourceLocation: SourceLocation,
        type: KotlinType,
        override val value: Int
) : IrLiteralBase(sourceLocation, type), IrIntLiteral {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitIntLiteral(this, data)
}

class IrStringLiteralImpl(
        sourceLocation: SourceLocation,
        type: KotlinType,
        override val value: String
) : IrLiteralBase(sourceLocation, type), IrStringLiteral {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitStringLiteral(this, data)
}