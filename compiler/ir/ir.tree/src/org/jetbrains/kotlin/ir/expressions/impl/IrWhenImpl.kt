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

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrWhenImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override val type: IrType,
    override val origin: IrStatementOrigin? = null
) : IrWhen() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        origin: IrStatementOrigin?,
        branches: List<IrBranch>
    ) : this(startOffset, endOffset, type, origin) {
        this.branches.addAll(branches)
    }

    override val branches: MutableList<IrBranch> = ArrayList()
}

open class IrBranchImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var condition: IrExpression,
    override var result: IrExpression
) : IrBranch() {
    constructor(condition: IrExpression, result: IrExpression) :
            this(condition.startOffset, condition.endOffset, condition, result)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBranch(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrBranch =
        transformer.visitBranch(this, data)
}

class IrElseBranchImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var condition: IrExpression,
    override var result: IrExpression
) : IrElseBranch() {
    constructor(condition: IrExpression, result: IrExpression) :
            this(condition.startOffset, condition.endOffset, condition, result)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitElseBranch(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElseBranch =
        transformer.visitElseBranch(this, data)
}
