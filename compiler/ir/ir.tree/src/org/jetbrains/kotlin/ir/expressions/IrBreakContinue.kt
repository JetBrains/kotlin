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
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrBreakContinue : IrExpression {
    var loop: IrLoop
    val label: String?
}

interface IrBreak: IrBreakContinue

interface IrContinue: IrBreakContinue

fun IrBreakContinue.getDepth(): Int {
    var depth = 0
    var finger: IrElement = this
    while (true) {
        val parent = finger.parent ?: throw AssertionError("No parent loop in tree for ${this.render()}:\n${finger.dump()}")
        if (parent is IrLoop) {
            if (parent == loop) {
                return depth
            }
            depth++
        }
        finger = parent
    }
}

abstract class IrBreakContinueBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override var loop: IrLoop
) : IrTerminalExpressionBase(startOffset, endOffset, type), IrBreakContinue {
    override var label: String? = null
}

class IrBreakImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        loop: IrLoop
) : IrBreakContinueBase(startOffset, endOffset, type, loop), IrBreak {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitBreak(this, data)
}

class IrContinueImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        loop: IrLoop
) : IrBreakContinueBase(startOffset, endOffset, type, loop), IrContinue {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitContinue(this, data)
}