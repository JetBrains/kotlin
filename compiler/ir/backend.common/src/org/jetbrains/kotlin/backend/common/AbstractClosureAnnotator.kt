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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

class Closure(val capturedValues: List<ValueDescriptor>)

@OptIn(DescriptorBasedIr::class)
abstract class AbstractClosureAnnotator : IrElementVisitorVoid {
    protected abstract fun recordFunctionClosure(functionDescriptor: FunctionDescriptor, closure: Closure)
    protected abstract fun recordClassClosure(classDescriptor: ClassDescriptor, closure: Closure)

    private class ClosureBuilder(val owner: DeclarationDescriptor) {
        val capturedValues = mutableSetOf<ValueDescriptor>()

        fun buildClosure() = Closure(capturedValues.toList())

        fun addNested(closure: Closure) {
            fillInNestedClosure(capturedValues, closure.capturedValues)
        }

        private fun <T : CallableDescriptor> fillInNestedClosure(destination: MutableSet<T>, nested: List<T>) {
            nested.filterTo(destination) {
                it.containingDeclaration != owner
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

    override fun visitFunction(declaration: IrFunction) {
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

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        // Getter and setter of local delegated properties are special generated functions and don't have closure.
        declaration.delegate.initializer?.acceptVoid(this)
    }

    override fun visitVariableAccess(expression: IrValueAccessExpression) {
        val closureBuilder = closuresStack.peek() ?: return

        val variableDescriptor = expression.symbol.descriptor
        if (variableDescriptor.containingDeclaration != closureBuilder.owner) {
            closureBuilder.capturedValues.add(variableDescriptor)
        }

        expression.acceptChildrenVoid(this)
    }

}
