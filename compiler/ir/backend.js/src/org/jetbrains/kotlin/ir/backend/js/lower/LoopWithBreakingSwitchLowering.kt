/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import java.util.*

class LoopWithBreakingSwitchLowering : BodyLoweringPass {

    private val switchInLoopTransformer = LoopWithBreakingSwitchTransformer()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        when (container) {
            is IrFunction -> {
                container.accept(switchInLoopTransformer, null)
            }
        }
    }
}

class LoopWithBreakingSwitchTransformer : IrElementTransformerVoid() {
    private var counter: Int = 0

    private val loopDeque: Deque<IrLoop> = LinkedList()

    override fun visitBreak(jump: IrBreak): IrExpression {
        val loop = loopDeque.firstOrNull()
        loop?.label = loop?.label ?: makeLoopLabel()

        return super.visitBreak(jump)
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        loopDeque.push(loop)
        return super.visitLoop(loop).apply {
            loopDeque.pop()
        }
    }

    private fun makeLoopLabel() = "\$l\$switch\$${counter++}"
}
