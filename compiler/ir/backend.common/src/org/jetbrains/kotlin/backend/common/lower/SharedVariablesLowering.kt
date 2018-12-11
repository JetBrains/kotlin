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

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

object CoroutineIntrinsicLambdaOrigin : IrStatementOriginImpl("Coroutine intrinsic lambda")

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
            irFunction.accept(object : IrElementVisitor<Unit, IrDeclarationParent?> {
                val relevantVars = mutableSetOf<IrVariable>()

                override fun visitElement(element: IrElement, data: IrDeclarationParent?) {
                    element.acceptChildren(this, data)
                }

                override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent?) =
                    super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

                override fun visitContainerExpression(expression: IrContainerExpression, data: IrDeclarationParent?) =
                    super.visitContainerExpression(
                        expression,
                        if (expression is IrReturnableBlock
                            && expression.origin == CoroutineIntrinsicLambdaOrigin
                        )
                            null
                        else
                            data
                    )

                override fun visitVariable(declaration: IrVariable, data: IrDeclarationParent?) {
                    declaration.acceptChildren(this, data)

                    if (declaration.isVar) {
                        relevantVars.add(declaration)
                    }
                }

                override fun visitValueAccess(expression: IrValueAccessExpression, data: IrDeclarationParent?) {
                    expression.acceptChildren(this, data)

                    val value = expression.symbol.owner
                    if (value in relevantVars && (value as IrVariable).parent != data) {
                        sharedVariables.add(value)
                    }
                }
            }, irFunction)
        }

        private fun rewriteSharedVariables() {
            val transformedDescriptors = HashMap<IrValueSymbol, IrVariableSymbol>()

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    if (declaration !in sharedVariables) return declaration

                    val newDeclaration = context.sharedVariablesManager.declareSharedVariable(declaration)
                    newDeclaration.parent = irFunction
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