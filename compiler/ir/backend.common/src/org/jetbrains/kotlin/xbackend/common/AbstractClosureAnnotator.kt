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

package org.jetbrains.kotlin.xbackend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrGeneralFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalPropertyAccessor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

class Closure(
        val capturedThisReferences: List<ClassDescriptor>,
        val capturedReceiverParameters: List<ReceiverParameterDescriptor>,
        val capturedVariables: List<VariableDescriptor>
)

abstract class AbstractClosureAnnotator : IrElementVisitorVoid {
    protected abstract fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure)
    protected abstract fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure)

    private class ClosureBuilder(val owner: DeclarationDescriptor) {
        val capturedThisReferences = mutableSetOf<ClassDescriptor>()
        val capturedReceiverParameters = mutableSetOf<ReceiverParameterDescriptor>()
        val capturedVariables = mutableSetOf<VariableDescriptor>()

        fun buildClosure() = Closure(
                capturedThisReferences.toList(),
                capturedReceiverParameters.toList(),
                capturedVariables.toList()
        )

        fun addNested(closure: Closure) {
            fillInCapturedThisReferences(closure)
            fillInNestedClosure(capturedReceiverParameters, closure.capturedReceiverParameters)
            fillInNestedClosure(capturedVariables, closure.capturedVariables)
        }

        private fun fillInCapturedThisReferences(closure: Closure) {
            if (owner is ClassDescriptor) {
                closure.capturedThisReferences.filterTo(capturedThisReferences) { it != owner }
            }
            else if (owner is CallableMemberDescriptor && owner.dispatchReceiverParameter != null) {
                val ownerClass = owner.containingDeclaration as? ClassDescriptor
                closure.capturedThisReferences.filterTo(capturedThisReferences) { it != ownerClass }
            }
        }

        private fun <T : CallableDescriptor> fillInNestedClosure(destination: MutableSet<T>, nested: List<T>) {
            nested.filterTo(destination) {
                it.containingDeclaration != owner
            }
        }

        fun addCapturedThis(classDescriptor: ClassDescriptor) {
            if (owner is ClassDescriptor && owner != classDescriptor) {
                capturedThisReferences.add(classDescriptor)
            }
            else if (owner is CallableMemberDescriptor && owner.containingDeclaration != classDescriptor) {
                capturedThisReferences.add(classDescriptor)
            }
        }
    }

    private val closuresStack = ArrayDeque<ClosureBuilder>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        val classDescriptor = declaration.descriptor
        val closureBuilder = ClosureBuilder(classDescriptor)

        closuresStack.push(closureBuilder)
        declaration.acceptChildrenVoid(this)
        closuresStack.pop()

        val closure = closureBuilder.buildClosure()

        if (DescriptorUtils.isLocal(classDescriptor)) {
            recordClassClosure(classDescriptor, closure)
        }

        closuresStack.peek()?.addNested(closure)
    }

    override fun visitGeneralFunction(declaration: IrGeneralFunction) {
        val functionDescriptor = declaration.descriptor
        val closureBuilder = ClosureBuilder(functionDescriptor)

        closuresStack.push(closureBuilder)
        declaration.acceptChildrenVoid(this)
        closuresStack.pop()

        val closure = closureBuilder.buildClosure()

        if (DescriptorUtils.isLocal(functionDescriptor)) {
            recordFunctionClosure(functionDescriptor, closure)
        }

        closuresStack.peek()?.addNested(closure)
    }

    override fun visitLocalPropertyAccessor(declaration: IrLocalPropertyAccessor) {
        // Local property accessors are created for delegated local properties and have no closure.
    }

    override fun visitThisReference(expression: IrThisReference) {
        closuresStack.peek().addCapturedThis(expression.classDescriptor)
    }

    override fun visitVariableAccess(expression: IrVariableAccessExpression) {
        val closureBuilder = closuresStack.peek()
        val variableDescriptor = expression.descriptor
        if (variableDescriptor.containingDeclaration != closureBuilder.owner) {
            closureBuilder.capturedVariables.add(variableDescriptor)
        }

        expression.acceptChildrenVoid(this)
    }

    override fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver) {
        val closureBuilder = closuresStack.peek()
        val receiverDescriptor = expression.descriptor
        if (receiverDescriptor.containingDeclaration != closureBuilder.owner) {
            closureBuilder.capturedReceiverParameters.add(receiverDescriptor)
        }
    }
}
