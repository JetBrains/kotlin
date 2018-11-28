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

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils

// TODO: synchronize with JVM BE
// TODO: rename the file.
class Closure(val capturedValues: List<IrValueSymbol> = emptyList())

class ClosureAnnotator(declaration: IrDeclaration) {
    private val closureBuilders = mutableMapOf<IrDeclaration, ClosureBuilder>()

    init {
        // Collect all closures for classes and functions. Collect call graph
        declaration.acceptChildrenVoid(ClosureCollectorVisitor())
    }

    fun getFunctionClosure(declaration: IrFunction) = getClosure(declaration)
    fun getClassClosure(declaration: IrClass) = getClosure(declaration)

    private fun getClosure(declaration: IrDeclaration): Closure {
        closureBuilders.values.forEach { it.processed = false }
        return closureBuilders
            .getOrElse(declaration) { throw AssertionError("No closure builder for passed descriptor.") }
            .buildClosure()
    }

    private class ClosureBuilder(val owner: IrDeclaration) {
        val capturedValues = mutableSetOf<IrValueSymbol>()
        private val declaredValues = mutableSetOf<IrValueDeclaration>()
        private val includes = mutableSetOf<ClosureBuilder>()

        var processed = false

        /*
         * Node's closure = variables captured by the node +
         *                  closure of all included nodes -
         *                  variables declared in the node.
         */
        fun buildClosure(): Closure {
            val result = mutableSetOf<IrValueSymbol>().apply { addAll(capturedValues) }
            includes.forEach { builder ->
                if (!builder.processed) {
                    builder.processed = true
                    builder.buildClosure().capturedValues.filterTo(result) { isExternal(it.owner) }
                }
            }
            // TODO: We can save the closure and reuse it.
            return Closure(result.toList())
        }


        fun include(includingBuilder: ClosureBuilder) {
            includes.add(includingBuilder)
        }

        fun declareVariable(valueDeclaration: IrValueDeclaration?) {
            if (valueDeclaration != null)
                declaredValues.add(valueDeclaration)
        }

        fun seeVariable(value: IrValueSymbol) {
            if (isExternal(value.owner))
                capturedValues.add(value)
        }

        fun isExternal(valueDeclaration: IrValueDeclaration): Boolean {
            return !declaredValues.contains(valueDeclaration)
        }
    }

    private inner class ClosureCollectorVisitor : IrElementVisitorVoid {

        val closuresStack = mutableListOf<ClosureBuilder>()

        fun includeInParent(builder: ClosureBuilder) {
            // We don't include functions or classes in a parent function when they are declared.
            // Instead we will include them when are is used (use = call for a function or constructor call for a class).
            val parentBuilder = closuresStack.peek()
            if (parentBuilder != null && parentBuilder.owner !is IrFunction) {
                parentBuilder.include(builder)
            }
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            val closureBuilder = ClosureBuilder(declaration)
            closureBuilders[declaration] = closureBuilder

            closureBuilder.declareVariable(declaration.thisReceiver)
            if (declaration.isInner) {
                closureBuilder.declareVariable((declaration.parent as IrClass).thisReceiver)
                includeInParent(closureBuilder)
            }

            declaration.declarations.firstOrNull { it is IrConstructor && it.isPrimary }?.let {
                val constructor = it as IrConstructor
                constructor.valueParameters.forEach { v -> closureBuilder.declareVariable(v) }
            }

            closuresStack.push(closureBuilder)
            declaration.acceptChildrenVoid(this)
            closuresStack.pop()
        }

        override fun visitFunction(declaration: IrFunction) {
            val closureBuilder = ClosureBuilder(declaration)
            closureBuilders[declaration] = closureBuilder

            declaration.valueParameters.forEach { closureBuilder.declareVariable(it) }
            closureBuilder.declareVariable(declaration.dispatchReceiverParameter)
            closureBuilder.declareVariable(declaration.extensionReceiverParameter)

            if (declaration is IrConstructor) {
                val constructedClass = (declaration.parent as IrClass)
                closureBuilder.declareVariable(constructedClass.thisReceiver)

                // Include closure of the class in the constructor closure.
                val classBuilder = closuresStack.peek()
                classBuilder?.let {
                    assert(classBuilder.owner == constructedClass)
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
            closuresStack.peek()?.declareVariable(declaration)
            super.visitVariable(declaration)
        }

        override fun visitCatch(aCatch: IrCatch) {
            closuresStack.peek()?.declareVariable(aCatch.catchParameter)
            super.visitCatch(aCatch)
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
            expression.acceptChildrenVoid(this)
            processMemberAccess(expression.symbol.owner)
        }

        override fun visitCall(expression: IrCall) {
            expression.acceptChildrenVoid(this)
            processMemberAccess(expression.symbol.owner)
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
            expression.acceptChildrenVoid(this)
            processMemberAccess(expression.symbol.owner)
        }

        override fun visitFunctionReference(expression: IrFunctionReference) {
            expression.acceptChildrenVoid(this)
            processMemberAccess(expression.symbol.owner)
        }

//        override fun visitPropertyReference(expression: IrPropertyReference) = processMemberAccess(expression.)

        private fun processMemberAccess(declaration: IrDeclaration) {
            if (DescriptorUtils.isLocal(declaration.descriptor)) {
                val builder = closureBuilders[declaration]
                builder?.let {
                    closuresStack.peek()?.include(builder)
                }
            }
        }
    }
}