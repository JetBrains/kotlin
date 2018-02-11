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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

class IrReturnImpl(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    override val returnTargetSymbol: IrFunctionSymbol,
    override var value: IrExpression
) : IrExpressionBase(startOffset, endOffset, type), IrReturn {
    constructor(startOffset: Int, endOffset: Int, returnTargetSymbol: IrFunctionSymbol, value: IrExpression) :
            this(startOffset, endOffset, returnTargetSymbol.descriptor.builtIns.nothingType, returnTargetSymbol, value)

    @Deprecated("Creates unbound symbol")
    constructor(startOffset: Int, endOffset: Int, type: KotlinType, returnTargetDescriptor: FunctionDescriptor, value: IrExpression) :
            this(
                startOffset, endOffset, type,
                createFunctionSymbol(returnTargetDescriptor),
                value
            )

    @Deprecated("Creates unbound symbol")
    constructor(startOffset: Int, endOffset: Int, returnTargetDescriptor: FunctionDescriptor, value: IrExpression) :
            this(
                startOffset, endOffset, returnTargetDescriptor.builtIns.nothingType,
                createFunctionSymbol(returnTargetDescriptor),
                value
            )

    override val returnTarget: FunctionDescriptor get() = returnTargetSymbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitReturn(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        value = value.transform(transformer, data)
    }
}