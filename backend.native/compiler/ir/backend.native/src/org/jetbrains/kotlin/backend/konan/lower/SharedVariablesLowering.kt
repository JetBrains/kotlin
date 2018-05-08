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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

// TODO: Fix .parent for variables and use from common backend.
class SharedVariablesLowering(val context: BackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        SharedVariablesTransformer(irFunction).lowerSharedVariables()
    }

    private inner class SharedVariablesTransformer(val irFunction: IrFunction) {
        val sharedVariables = mutableSetOf<IrVariable>()

        fun lowerSharedVariables() {
            collectSharedVariables()
            if (sharedVariables.isEmpty()) return

            rewriteSharedVariables()
        }

        private fun collectSharedVariables() {
            irFunction.acceptVoid(object : IrElementVisitorVoid {
                val relevantVars = mutableSetOf<IrVariable>()
                val functionsVariables = mutableListOf<MutableSet<IrVariable>>()

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    functionsVariables.push(mutableSetOf())
                    declaration.acceptChildrenVoid(this)
                    functionsVariables.pop()
                }

                override fun visitVariable(declaration: IrVariable) {
                    declaration.acceptChildrenVoid(this)

                    if (declaration.isVar) {
                        relevantVars.add(declaration)
                        functionsVariables.peek()!!.add(declaration)
                    }
                }

                override fun visitVariableAccess(expression: IrValueAccessExpression) {
                    expression.acceptChildrenVoid(this)

                    val value = expression.symbol.owner
                    //if (descriptor in relevantVars && descriptor.containingDeclaration != currentDeclaration) {
                    // TODO: fix lowerings to match check `(value as IrVariable).parent != currentDeclaration`
                    if (value in relevantVars && !functionsVariables.peek()!!.contains(value)) {
                        sharedVariables.add(value as IrVariable)
                    }
                }
            })
        }

        private fun rewriteSharedVariables() {
            val transformedVariableSymbols = HashMap<IrValueSymbol, IrVariableSymbol>()

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    if (declaration !in sharedVariables) return declaration

                    val newDeclaration = context.sharedVariablesManager.declareSharedVariable(declaration)
                    transformedVariableSymbols[declaration.symbol] = newDeclaration.symbol

                    return context.sharedVariablesManager.defineSharedValue(declaration, newDeclaration)
                }
            })

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newSymbol = getTransformedSymbol(expression.symbol) ?: return expression

                    return context.sharedVariablesManager.getSharedValue(newSymbol, expression)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newDescriptor = getTransformedSymbol(expression.symbol) ?: return expression

                    return context.sharedVariablesManager.setSharedValue(newDescriptor, expression)
                }

                private fun getTransformedSymbol(oldSymbol: IrValueSymbol): IrVariableSymbol? =
                        transformedVariableSymbols.getOrElse(oldSymbol) {
                            assert(oldSymbol.owner !in sharedVariables) {
                                "Shared variable is not transformed: $oldSymbol"
                            }
                            null
                        }
            })
        }
    }
}