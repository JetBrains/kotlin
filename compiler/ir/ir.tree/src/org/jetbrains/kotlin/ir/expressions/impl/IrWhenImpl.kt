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

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import java.util.*

abstract class IrWhenBase(startOffset: Int, endOffset: Int, type: KotlinType, override val origin: IrStatementOrigin? = null) :
    IrExpressionBase(startOffset, endOffset, type), IrWhen {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitWhen(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        branches.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        branches.forEachIndexed { i, irBranch ->
            branches[i] = irBranch.transform(transformer, data)
        }
    }
}

class IrWhenImpl(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    override val origin: IrStatementOrigin? = null
) : IrWhenBase(startOffset, endOffset, type) {
    constructor(
        startOffset: Int, endOffset: Int, type: KotlinType, origin: IrStatementOrigin?,
        branches: List<IrBranch>
    ) : this(startOffset, endOffset, type, origin) {
        this.branches.addAll(branches)
    }

    override val branches: MutableList<IrBranch> = ArrayList()
}

open class IrBranchImpl(startOffset: Int, endOffset: Int, override var condition: IrExpression, override var result: IrExpression) :
    IrElementBase(startOffset, endOffset), IrBranch {
    constructor(condition: IrExpression, result: IrExpression) : this(condition.startOffset, condition.endOffset, condition, result)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        condition = condition.transform(transformer, data)
        result = result.transform(transformer, data)
    }

    companion object {
        fun elseBranch(result: IrExpression) =
            IrElseBranchImpl(
                IrConstImpl.boolean(result.startOffset, result.endOffset, result.type.builtIns.booleanType, true),
                result
            )
    }
}

class IrElseBranchImpl(startOffset: Int, endOffset: Int, condition: IrExpression, result: IrExpression) :
    IrBranchImpl(startOffset, endOffset, condition, result), IrElseBranch {
    constructor(condition: IrExpression, result: IrExpression) : this(condition.startOffset, condition.endOffset, condition, result)
}