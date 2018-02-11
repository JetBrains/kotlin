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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

abstract class IrMemberAccessExpressionBase(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    val typeArguments: Map<TypeParameterDescriptor, KotlinType>?
) : IrExpressionBase(startOffset, endOffset, type), IrMemberAccessExpression {
    override var dispatchReceiver: IrExpression? = null
    override var extensionReceiver: IrExpression? = null

    override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): KotlinType? =
        typeArguments?.get(typeParameterDescriptor)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
    }
}