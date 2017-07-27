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

package org.jetbrains.kotlin.ir2cfg.generators

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph
import org.jetbrains.kotlin.ir2cfg.nodes.MergeCfgElement

class FunctionGenerator(val function: IrFunction) {

    val builder = FunctionBuilder(function)

    val exit = MergeCfgElement(function, "Function exit")

    val loopEntries = mutableMapOf<IrLoop, IrStatement>()

    val loopExits = mutableMapOf<IrLoop, IrStatement>()

    fun generate(): ControlFlowGraph {
        val visitor = FunctionVisitor()
        function.accept(visitor, true)
        return builder.build()
    }

    inner class FunctionVisitor : IrElementVisitor<IrStatement?, Boolean> {

        private inline fun <reified IE : IrElement> IE.process(includeSelf: Boolean = true) = this.accept(this@FunctionVisitor, includeSelf)

        override fun visitFunction(declaration: IrFunction, data: Boolean): IrStatement? {
            if (data) {
                builder.add(declaration)
            }
            val result = declaration.body?.process() ?: if (data) declaration else null
            if (result != null && !result.isNothing()) {
                builder.jump(exit, from = result)
            }
            return result
        }

        private fun IrStatementContainer.process(): IrStatement? {
            var result: IrStatement? = null
            for (statement in statements) {
                result = statement.process()
            }
            return result
        }

        private fun IrElement?.isNothing() = this is IrExpression && KotlinBuiltIns.isNothing(type)

        override fun visitBlockBody(body: IrBlockBody, data: Boolean): IrStatement? {
            return body.process()
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: Boolean): IrStatement? {
            return expression.process() ?: expression
        }

        override fun visitVariable(declaration: IrVariable, data: Boolean): IrStatement? {
            declaration.initializer?.process()
            return if (data) {
                builder.add(declaration)
                declaration
            }
            else null
        }

        override fun visitReturn(expression: IrReturn, data: Boolean): IrStatement? {
            expression.value.process()
            if (data) {
                builder.add(expression)
            }
            builder.jump(exit)
            return expression
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Boolean): IrStatement? {
            return body.expression.process()
        }

        override fun visitExpression(expression: IrExpression, data: Boolean): IrStatement? {
            if (data) {
                builder.add(expression)
            }
            return expression
        }

        override fun visitWhen(expression: IrWhen, data: Boolean): IrStatement? {
            if (data) {
                builder.add(expression)
            }
            val whenExit = MergeCfgElement(expression, "When exit")
            val branches = expression.branches
            for (branch in branches) {
                val condition = branch.condition
                condition.process(includeSelf = false)
                builder.jump(condition)
            }
            for (branch in branches) {
                val result = branch.result
                builder.move(branch.condition)
                if (!result.process().isNothing()) {
                    builder.jump(whenExit)
                }
                else {
                    builder.move(branch.condition)
                }
            }
            return whenExit
        }

        override fun visitWhileLoop(loop: IrWhileLoop, data: Boolean): IrStatement? {
            if (data) {
                builder.add(loop)
            }
            val exit = MergeCfgElement(loop, "While exit")
            loopExits[loop] = exit
            val entry = MergeCfgElement(loop, "While entry")
            loopEntries[loop] = entry
            builder.jump(entry)
            val condition = loop.condition
            condition.process(includeSelf = false)
            builder.jump(condition)
            val body = loop.body
            if (!body?.process().isNothing()) {
                builder.jump(entry)
            }
            builder.jump(exit, from = condition)
            return exit
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Boolean): IrStatement? {
            if (data) {
                builder.add(loop)
            }
            val exit = MergeCfgElement(loop, "Do..while exit")
            loopExits[loop] = exit
            val entry = MergeCfgElement(loop, "Do..while entry")
            loopEntries[loop] = entry
            builder.jump(entry)
            val body = loop.body
            val condition = loop.condition
            if (!body?.process().isNothing()) {
                condition.process(includeSelf = false)
                builder.jump(condition)
                builder.jump(entry, from = condition)
                builder.jump(exit, from = condition)
            }
            builder.move(exit)
            return exit
        }

        override fun visitBreak(jump: IrBreak, data: Boolean): IrStatement? {
            if (data) {
                builder.add(jump)
            }
            builder.jump(loopExits[jump.loop] ?: throw AssertionError("Loop exit not found for ${jump.loop.dump()}"))
            return jump
        }

        override fun visitContinue(jump: IrContinue, data: Boolean): IrStatement? {
            if (data) {
                builder.add(jump)
            }
            builder.jump(loopEntries[jump.loop] ?: throw AssertionError("Loop entry not found for ${jump.loop.dump()}"))
            return jump
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression, data: Boolean): IrStatement? {
            expression.dispatchReceiver?.process()
            expression.extensionReceiver?.process()
            for (valueParameter in expression.descriptor.valueParameters) {
                expression.getValueArgument(valueParameter)?.process()
            }
            if (data) {
                builder.add(expression)
            }
            return expression
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Boolean): IrStatement? {
            expression.argument.process()
            if (data) {
                builder.add(expression)
            }
            return expression
        }

        override fun visitElement(element: IrElement, data: Boolean): IrStatement? {
            TODO("not implemented")
        }
    }
}