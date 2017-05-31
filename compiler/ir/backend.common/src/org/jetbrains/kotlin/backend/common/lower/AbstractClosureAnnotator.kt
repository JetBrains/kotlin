/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils

// TODO: synchronize with JVM BE
// TODO: rename the file.
class Closure(val capturedValues: List<IrValueSymbol> = emptyList())

class ClosureAnnotator   {
    private val closureBuilders = mutableMapOf<DeclarationDescriptor, ClosureBuilder>()

    constructor(declaration: IrDeclaration)  {
        // Collect all closures for classes and functions. Collect call graph
        declaration.acceptChildrenVoid(ClosureCollectorVisitor())
    }

    fun getFunctionClosure(descriptor: FunctionDescriptor) = getClosure(descriptor)
    fun getClassClosure(descriptor: ClassDescriptor) = getClosure(descriptor)

    private fun getClosure(descriptor: DeclarationDescriptor) : Closure {
        closureBuilders.values.forEach { it.processed = false }
        return closureBuilders
                .getOrElse(descriptor) { throw AssertionError("No closure builder for passed descriptor.") }
                .buildClosure()
    }

    private class ClosureBuilder(val owner: DeclarationDescriptor) {
        val capturedValues = mutableSetOf<IrValueSymbol>()
        private val declaredValues = mutableSetOf<ValueDescriptor>()
        private val includes = mutableSetOf<ClosureBuilder>()

        var processed = false

        /*
         * Node's closure = variables captured by the node +
         *                  closure of all included nodes -
         *                  variables declared in the node.
         */
        fun buildClosure(): Closure {
            val result = mutableSetOf<IrValueSymbol>().apply { addAll(capturedValues) }
            includes.forEach {
                if (!it.processed) {
                    it.processed = true
                    it.buildClosure().capturedValues.filterTo(result) { isExternal(it.descriptor) }
                }
            }
            // TODO: We can save the closure and reuse it.
            return Closure(result.toList())
        }


        fun include(includingBuilder: ClosureBuilder) {
            includes.add(includingBuilder)
        }

        fun declareVariable(valueDescriptor: ValueDescriptor?) {
            if (valueDescriptor != null)
                declaredValues.add(valueDescriptor)
        }

        fun seeVariable(value: IrValueSymbol) {
            if (isExternal(value.descriptor))
                capturedValues.add(value)
        }

        fun isExternal(valueDescriptor: ValueDescriptor): Boolean {
            return !declaredValues.contains(valueDescriptor)
        }

    }

    private inner class ClosureCollectorVisitor : IrElementVisitorVoid {

        val closuresStack = mutableListOf<ClosureBuilder>()

        fun includeInParent(builder: ClosureBuilder) {
            // We don't include functions or classes in a parent function when they are declared.
            // Instead we will include them when are is used (use = call for a function or constructor call for a class).
            val parentBuilder = closuresStack.peek()
            if (parentBuilder != null && parentBuilder.owner !is FunctionDescriptor) {
                parentBuilder.include(builder)
            }
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            val classDescriptor = declaration.descriptor
            val closureBuilder = ClosureBuilder(classDescriptor)
            closureBuilders[declaration.descriptor] = closureBuilder

            closureBuilder.declareVariable(classDescriptor.thisAsReceiverParameter)
            if (classDescriptor.isInner) {
                closureBuilder.declareVariable((classDescriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter)
                includeInParent(closureBuilder)
            }

            closuresStack.push(closureBuilder)
            declaration.acceptChildrenVoid(this)
            closuresStack.pop()
        }

        override fun visitFunction(declaration: IrFunction) {
            val functionDescriptor = declaration.descriptor
            val closureBuilder = ClosureBuilder(functionDescriptor)
            closureBuilders[functionDescriptor] = closureBuilder

            functionDescriptor.valueParameters.forEach { closureBuilder.declareVariable(it) }
            closureBuilder.declareVariable(functionDescriptor.dispatchReceiverParameter)
            closureBuilder.declareVariable(functionDescriptor.extensionReceiverParameter)
            if (functionDescriptor is ConstructorDescriptor) {
                closureBuilder.declareVariable(functionDescriptor.constructedClass.thisAsReceiverParameter)
                // Include closure of the class in the constructor closure.
                val classBuilder = closuresStack.peek()
                classBuilder?.let {
                    assert(classBuilder.owner == functionDescriptor.constructedClass)
                    closureBuilder.include(classBuilder)
                }
            }

            closuresStack.push(closureBuilder)
            declaration.acceptChildrenVoid(this)
            closuresStack.pop()

            includeInParent(closureBuilder)
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
            // Getter and setter of local delegated properties are special generated functions and don't have closure.
            declaration.delegate.initializer?.acceptVoid(this)
        }

        override fun visitVariableAccess(expression: IrValueAccessExpression) {
            closuresStack.peek()?.seeVariable(expression.symbol)
            super.visitVariableAccess(expression)
        }

        override fun visitVariable(declaration: IrVariable) {
            closuresStack.peek()?.declareVariable(declaration.descriptor)
            super.visitVariable(declaration)
        }

        override fun visitCatch(aCatch: IrCatch) {
            closuresStack.peek()?.declareVariable(aCatch.parameter)
            super.visitCatch(aCatch)
        }

        // Process delegating constructor calls, enum constructor calls, calls and callable references.
        override fun visitMemberAccess(expression: IrMemberAccessExpression) {
            expression.acceptChildrenVoid(this)
            val descriptor = expression.descriptor
            if (DescriptorUtils.isLocal(descriptor)) {
                val builder = closureBuilders[descriptor]
                builder?.let {
                    closuresStack.peek()?.include(builder)
                }
            }
        }
    }
}