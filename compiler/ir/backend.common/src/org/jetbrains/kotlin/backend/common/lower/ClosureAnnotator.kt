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

import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

data class Closure(val capturedValues: List<IrValueSymbol>, val capturedTypeParameters: List<IrTypeParameter>)

class ClosureAnnotator(irElement: IrElement, declaration: IrDeclaration) {
    private val closureBuilders = mutableMapOf<IrDeclaration, ClosureBuilder>()

    init {
        // Collect all closures for classes and functions. Collect call graph
        irElement.accept(ClosureCollectorVisitor(), declaration.closureBuilderOrNull ?: declaration.parentClosureBuilder)
    }

    fun getFunctionClosure(declaration: IrFunction) = getClosure(declaration)
    fun getClassClosure(declaration: IrClass) = getClosure(declaration)

    private fun getClosure(declaration: IrDeclaration): Closure {
        closureBuilders.values.forEach { it.processed = false }
        return closureBuilders
            .getOrElse(declaration) { throw AssertionError("No closure builder for passed declaration ${ir2string(declaration)}.") }
            .buildClosure()
    }

    private class ClosureBuilder(val owner: IrDeclaration) {
        private val capturedValues = mutableSetOf<IrValueSymbol>()
        private val declaredValues = mutableSetOf<IrValueDeclaration>()
        private val includes = mutableSetOf<ClosureBuilder>()

        private val potentiallyCapturedTypeParameters = mutableSetOf<IrTypeParameter>()
        private val capturedTypeParameters = mutableSetOf<IrTypeParameter>()

        var processed = false

        /*
         * Node's closure = variables captured by the node +
         *                  closure of all included nodes -
         *                  variables declared in the node.
         */
        fun buildClosure(): Closure {
            includes.forEach { builder ->
                if (!builder.processed) {
                    builder.processed = true
                    val subClosure = builder.buildClosure()
                    subClosure.capturedValues.filterTo(capturedValues) { isExternal(it.owner) }
                    subClosure.capturedTypeParameters.filterTo(capturedTypeParameters) { isExternal(it) }
                }
            }
            // TODO: We can save the closure and reuse it.
            return Closure(capturedValues.toList(), capturedTypeParameters.toList())
        }


        fun include(includingBuilder: ClosureBuilder) {
            includes.add(includingBuilder)
        }

        fun declareVariable(valueDeclaration: IrValueDeclaration?) {
            if (valueDeclaration != null) {
                declaredValues.add(valueDeclaration)
                seeType(valueDeclaration.type)
            }
        }

        fun seeVariable(value: IrValueSymbol) {
            if (isExternal(value.owner)) {
                capturedValues.add(value)
            }
        }

        fun isExternal(valueDeclaration: IrValueDeclaration): Boolean {
            return !declaredValues.contains(valueDeclaration)
        }

        fun isExternal(typeParameter: IrTypeParameter): Boolean {
            return potentiallyCapturedTypeParameters.contains(typeParameter)
        }

        fun addPotentiallyCapturedTypeParameter(param: IrTypeParameter) {
            potentiallyCapturedTypeParameters.add(param)
        }

        fun seeType(type: IrType) {
            if (type !is IrSimpleType) return
            val classifier = type.classifier
            if (classifier is IrTypeParameterSymbol && isExternal(classifier.owner) && capturedTypeParameters.add(classifier.owner))
                classifier.owner.superTypes.forEach(::seeType)
            type.arguments.forEach {
                (it as? IrTypeProjection)?.type?.let(::seeType)
            }
            type.abbreviation?.arguments?.forEach {
                (it as? IrTypeProjection)?.type?.let(::seeType)
            }
        }
    }

    private fun includeInParent(builder: ClosureBuilder) {
        // We don't include functions or classes in a parent function when they are declared.
        // Instead we will include them when are is used (use = call for a function or constructor call for a class).
        val parentBuilder = builder.owner.parentClosureBuilder
        if (parentBuilder != null && parentBuilder.owner !is IrFunction) {
            parentBuilder.include(builder)
        }
    }

    private val IrClass.closureBuilder: ClosureBuilder
        get() = closureBuilders.getOrPut(this) {
            val closureBuilder = ClosureBuilder(this)

            collectPotentiallyCapturedTypeParameters(closureBuilder)

            closureBuilder.declareVariable(this.thisReceiver)
            if (this.isInner) {
                closureBuilder.declareVariable((this.parent as IrClass).thisReceiver)
                includeInParent(closureBuilder)
            }

            this.declarations.firstOrNull { it is IrConstructor && it.isPrimary }?.let {
                val constructor = it as IrConstructor
                constructor.valueParameters.forEach { v -> closureBuilder.declareVariable(v) }
            }

            closureBuilder
        }

    private val IrFunction.closureBuilder: ClosureBuilder
        get() = closureBuilders.getOrPut(this) {
            val closureBuilder = ClosureBuilder(this)

            collectPotentiallyCapturedTypeParameters(closureBuilder)

            this.valueParameters.forEach { closureBuilder.declareVariable(it) }
            closureBuilder.declareVariable(this.dispatchReceiverParameter)
            closureBuilder.declareVariable(this.extensionReceiverParameter)

            if (this is IrConstructor) {
                val constructedClass = (this.parent as IrClass)
                closureBuilder.declareVariable(constructedClass.thisReceiver)

                // Include closure of the class in the constructor closure.
                val classBuilder = constructedClass.closureBuilder
                closureBuilder.include(classBuilder)
            }

            closureBuilder
        }

    private fun collectPotentiallyCapturedTypeParameters(closureBuilder: ClosureBuilder) {

        fun ClosureBuilder.doCollect() {
            if (owner !is IrClass) {
                (owner as? IrTypeParametersContainer)?.let { container ->
                    for (tp in container.typeParameters) {
                        closureBuilder.addPotentiallyCapturedTypeParameter(tp)
                    }
                }

                owner.parentClosureBuilder?.doCollect()
            }
        }

        closureBuilder.owner.parentClosureBuilder?.doCollect()
    }

    private val IrDeclaration.parentClosureBuilder: ClosureBuilder?
        get() = when (val p = parent) {
            is IrClass -> p.closureBuilder
            is IrFunction -> p.closureBuilder
            is IrDeclaration -> p.parentClosureBuilder
            else -> null
        }

    private val IrDeclaration.closureBuilderOrNull: ClosureBuilder?
        get() = when (this) {
            is IrClass -> closureBuilder
            is IrFunction -> closureBuilder
            else -> null
        }

    private inner class ClosureCollectorVisitor() : IrElementVisitor<Unit, ClosureBuilder?> {

        override fun visitElement(element: IrElement, data: ClosureBuilder?) {
            element.acceptChildren(this, data)
        }

        override fun visitClass(declaration: IrClass, data: ClosureBuilder?) {
            declaration.acceptChildren(this, declaration.closureBuilder)
        }

        override fun visitFunction(declaration: IrFunction, data: ClosureBuilder?) {
            val closureBuilder = declaration.closureBuilder

            declaration.acceptChildren(this, closureBuilder)

            includeInParent(closureBuilder)
        }

        override fun visitValueAccess(expression: IrValueAccessExpression, data: ClosureBuilder?) {
            data?.seeVariable(expression.symbol)
            super.visitValueAccess(expression, data)
        }

        override fun visitVariable(declaration: IrVariable, data: ClosureBuilder?) {
            data?.declareVariable(declaration)
            super.visitVariable(declaration, data)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: ClosureBuilder?) {
            super.visitFunctionAccess(expression, data)
            processMemberAccess(expression.symbol.owner, data)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: ClosureBuilder?) {
            super.visitFunctionReference(expression, data)
            processMemberAccess(expression.symbol.owner, data)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: ClosureBuilder?) {
            super.visitFunctionExpression(expression, data)
            processMemberAccess(expression.function, data)
        }

        override fun visitPropertyReference(expression: IrPropertyReference, data: ClosureBuilder?) {
            super.visitPropertyReference(expression, data)
            expression.getter?.let { processMemberAccess(it.owner, data) }
            expression.setter?.let { processMemberAccess(it.owner, data) }
        }

        override fun visitExpression(expression: IrExpression, data: ClosureBuilder?) {
            super.visitExpression(expression, data)
            val typeParameterContainerScopeBuilder = data?.let {
                (it.owner as? IrConstructor)?.closureBuilder ?: it
            }
            typeParameterContainerScopeBuilder?.seeType(expression.type)
        }

        private fun processMemberAccess(declaration: IrDeclaration, parentClosure: ClosureBuilder?) {
            if (declaration.isLocal) {
                if (declaration is IrSimpleFunction && declaration.visibility != Visibilities.LOCAL) {
                    return
                }

                val builder = declaration.closureBuilderOrNull
                builder?.let {
                    parentClosure?.include(builder)
                }
            }
        }
    }
}