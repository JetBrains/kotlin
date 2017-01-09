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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

class SharedVariablesLowering(val context: BackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        SharedVariablesTransformer(irFunction).lowerSharedVariables()
    }

    private inner class SharedVariablesTransformer(val irFunction: IrFunction) {
        val sharedVariables = HashSet<ValueDescriptor>()

        fun lowerSharedVariables() {
            collectSharedVariables()
            if (sharedVariables.isEmpty()) return

            rewriteSharedVariables()
        }

        private fun collectSharedVariables() {
            irFunction.acceptVoid(object : IrElementVisitorVoid {
                val declarationsStack = ArrayDeque<IrDeclaration>()
                val currentDeclaration: DeclarationDescriptor
                    get() = declarationsStack.peek().descriptor

                val relevantVars = HashSet<VariableDescriptor>()

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

                    val variableDescriptor = declaration.descriptor
                    if (variableDescriptor.isVar) {
                        relevantVars.add(variableDescriptor)
                    }
                }

                override fun visitVariableAccess(expression: IrValueAccessExpression) {
                    expression.acceptChildrenVoid(this)

                    val descriptor = expression.descriptor
                    if (descriptor in relevantVars && descriptor.containingDeclaration != currentDeclaration) {
                        sharedVariables.add(descriptor)
                    }
                }
            })
        }

        private fun rewriteSharedVariables() {
            val transformedDescriptors = HashMap<ValueDescriptor, VariableDescriptor>()

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val oldDescriptor = declaration.descriptor
                    if (oldDescriptor !in sharedVariables) return declaration

                    val newDescriptor = context.sharedVariablesManager.createSharedVariableDescriptor(oldDescriptor)
                    transformedDescriptors[oldDescriptor] = newDescriptor

                    return context.sharedVariablesManager.defineSharedValue(newDescriptor, declaration)
                }
            })

            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newDescriptor = getTransformedDescriptor(expression.descriptor) ?: return expression

                    return context.sharedVariablesManager.getSharedValue(newDescriptor, expression)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newDescriptor = getTransformedDescriptor(expression.descriptor) ?: return expression

                    return context.sharedVariablesManager.setSharedValue(newDescriptor, expression)
                }

                private fun getTransformedDescriptor(oldDescriptor: ValueDescriptor): VariableDescriptor? =
                        transformedDescriptors.getOrElse(oldDescriptor) {
                            assert(oldDescriptor !in sharedVariables) {
                                "Shared variable is not transformed: $oldDescriptor"
                            }
                            null
                        }
            })
        }
    }
}