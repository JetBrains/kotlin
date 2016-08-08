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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.types.KotlinType


interface IrExpression : IrElement {
    override val parent: IrExpressionOwner?
    val index: Int
    val type: KotlinType

    fun setTreeLocation(parent: IrExpressionOwner?, index: Int)

    companion object {
        const val DETACHED_INDEX = Int.MIN_VALUE
    }
}

fun IrExpression.detach() {
    setTreeLocation(null, IrExpression.DETACHED_INDEX)
}

fun IrExpressionOwner.validateChild(child: IrExpression) {
    assert(child.parent == this && getChildExpression(child.index) == child) { "Inconsistent child: $child" }
}

abstract class IrExpressionBase(
        sourceLocation: SourceLocation,
        override val type: KotlinType
) : IrElementBase(sourceLocation), IrExpression {
    override var parent: IrExpressionOwner? = null
    override var index: Int = IrExpression.DETACHED_INDEX

    override fun setTreeLocation(parent: IrExpressionOwner?, index: Int) {
        this.parent = parent
        this.index = index
    }
}