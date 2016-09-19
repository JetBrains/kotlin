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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph
import org.jetbrains.kotlin.ir2cfg.nodes.MergeCfgElement

class FunctionGenerator(val function: IrFunction) {

    val builder = FunctionBuilder(function)

    val exit = MergeCfgElement(function, "Function exit")

    fun generate(): ControlFlowGraph {
        val visitor = FunctionVisitor()
        function.accept(visitor, true)
        return builder.build()
    }

    inner class FunctionVisitor : IrElementVisitor<IrElement?, Boolean> {

        inline fun <reified IE : IrElement> IE.process(includeSelf: Boolean = true) = this.accept(this@FunctionVisitor, includeSelf)

        override fun visitFunction(declaration: IrFunction, data: Boolean): IrElement? {
            if (data) {
                builder.add(declaration)
            }
            val result = declaration.body?.process()
            if (result != null && !result.isNothing()) {
                builder.jump(exit, from = result)
            }
            return result
        }

        private fun IrStatementContainer.process(): IrElement? {
            var result: IrElement? = null
            for (statement in statements) {
                result = statement.process()
            }
            return result
        }

        private fun IrElement?.isNothing() = this is IrExpression && KotlinBuiltIns.isNothing(type)

        override fun visitBlockBody(body: IrBlockBody, data: Boolean): IrElement? {
            return body.process() ?: body
        }

        override fun visitBlock(expression: IrBlock, data: Boolean): IrElement? {
            return expression.process() ?: expression
        }

        override fun visitVariable(declaration: IrVariable, data: Boolean): IrElement? {
            declaration.initializer?.process()
            return if (data) {
                builder.add(declaration)
                declaration
            }
            else null
        }

        override fun visitReturn(expression: IrReturn, data: Boolean): IrElement? {
            expression.value.process()
            if (data) {
                builder.add(expression)
            }
            builder.jump(exit)
            return expression
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: Boolean): IrElement? {
            if (data) {
                builder.add(body)
            }
            return body.expression.process()
        }

        override fun visitExpression(expression: IrExpression, data: Boolean): IrElement? {
            if (data) {
                builder.add(expression)
            }
            return expression
        }

        override fun visitElement(element: IrElement, data: Boolean): IrElement? {
            TODO("not implemented")
        }
    }
}