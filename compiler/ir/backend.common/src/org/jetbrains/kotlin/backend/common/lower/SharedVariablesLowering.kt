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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

class SharedVariablesLowering(val context: BackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        SharedVariablesTransformer(irFunction).lowerSharedVariables()
    }

    private inner class SharedVariablesTransformer(val irFunction: IrFunction) {
        private val sharedVariables = HashSet<IrVariable>()

        fun lowerSharedVariables() {
            collectSharedVariables()
            if (sharedVariables.isEmpty()) return

            rewriteSharedVariables()
        }

        private fun collectSharedVariables() {
            irFunction.acceptVoid(object : IrElementVisitorVoid {
                val declarationsStack = ArrayDeque<IrDeclaration>()
                val currentDeclaration: IrDeclaration
                    get() = declarationsStack.peek()

                val relevantVars = HashSet<IrVariable>()

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declarationsStack.push(declaration)
                    declaration.acceptChildrenVoid(this)
                    declarationsStack.pop()
                }

                override fun visitVariable(declaration: IrVariable) {
                    declaration.acceptChildrenVoid(this)

                    if (declaration.isVar) {
                        relevantVars.add(declaration)
                    }
                }

                override fun visitVariableAccess(expression: IrValueAccessExpression) {
                    expression.acceptChildrenVoid(this)

                    val value = expression.symbol.owner
                    if (value in relevantVars && (value as IrVariable).parent != currentDeclaration) {
                        sharedVariables.add(value)
                    }
                }
            })
        }

        private fun rewriteSharedVariables() {
            val transformedDescriptors = HashMap<IrValueSymbol, IrVariableSymbol>()

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    if (declaration !in sharedVariables) return declaration

                    val newDeclaration = context.sharedVariablesManager.declareSharedVariable(declaration)
                    transformedDescriptors[declaration.symbol] = newDeclaration.symbol

                    return context.sharedVariablesManager.defineSharedValue(declaration, newDeclaration)
                }
            })

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newDeclaration = getTransformedSymbol(expression.symbol) ?: return expression

                    return context.sharedVariablesManager.getSharedValue(newDeclaration, expression)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newDeclaration = getTransformedSymbol(expression.symbol) ?: return expression

                    return context.sharedVariablesManager.setSharedValue(newDeclaration, expression)
                }

                private fun getTransformedSymbol(oldSymbol: IrValueSymbol): IrVariableSymbol? =
                    transformedDescriptors.getOrElse(oldSymbol) {
                        assert(oldSymbol.owner !in sharedVariables) {
                            "Shared variable is not transformed: ${oldSymbol.owner.dump()}"
                        }
                        null
                    }
            })
        }
    }
}