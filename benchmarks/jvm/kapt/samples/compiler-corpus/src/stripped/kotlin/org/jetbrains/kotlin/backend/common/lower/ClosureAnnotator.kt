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

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.ir2string
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

data class Closure(val capturedValues: List<IrValueSymbol>, val capturedTypeParameters: List<IrTypeParameter>)

class ClosureAnnotator private constructor(
    irElement: IrElement,
    declaration: IrDeclaration?,
    private val closureBuilders: MutableMap<IrDeclaration, ClosureBuilder>,
    private val scriptingMode: Boolean,
    private val closestParent: IrDeclaration?,
) {

    constructor(
        irElement: IrElement,
        declaration: IrDeclaration,
        closureBuilders: MutableMap<IrDeclaration, ClosureBuilder> = mutableMapOf(),
        scriptingMode: Boolean = false,
    ) : this(irElement, declaration, closureBuilders, scriptingMode, null)

    constructor(
        irElement: IrElement,
        closureBuilders: MutableMap<IrDeclaration, ClosureBuilder>,
        closestParent: IrDeclaration?,
    ) : this(irElement, null, closureBuilders, false, closestParent)

    private val rootClosureBuilder = declaration?.closureBuilderOrNull ?: declaration?.parentClosureBuilder ?: ClosureBuilder(null).apply {
        collectPotentiallyCapturedTypeParameters(this)
    }

    val rootClosure: Closure
        get() = rootClosureBuilder.buildClosure()

    init {
        // Collect all closures for classes and functions. Collect call graph
        irElement.accept(ClosureCollectorVisitor(), rootClosureBuilder)
    }

    fun getFunctionClosure(declaration: IrFunction) = getClosure(declaration)
    fun getClassClosure(declaration: IrClass) = getClosure(declaration)

    fun getClosure(declaration: IrDeclaration): Closure { TODO() }

    class ClosureBuilder(val owner: IrDeclaration?) {
        private val capturedValues = mutableSetOf<IrValueSymbol>()
        private val declaredValues = mutableSetOf<IrValueDeclaration>()
        private val includes = mutableSetOf<ClosureBuilder>()

        private val potentiallyCapturedTypeParameters = mutableSetOf<IrTypeParameter>()
        private val capturedTypeParameters = mutableSetOf<IrTypeParameter>()

        private var closure: Closure? = null

        /*
         * This will solve a system of equations for each dependent closure:
         *
         *  closure[V] = captured(V) + { closure[U] | U <- included(V) } - declared(V)
         *
         */
        fun buildClosure(): Closure { TODO() }

        private fun collectConnectedClosures(): List<ClosureBuilder> { TODO() }

        private fun updateFromIncluded(): Boolean { TODO() }


        fun include(includingBuilder: ClosureBuilder) { TODO() }

        fun declareVariable(valueDeclaration: IrValueDeclaration?) { TODO() }

        fun seeVariable(value: IrValueSymbol) { TODO() }

        fun isExternal(valueDeclaration: IrValueDeclaration): Boolean { TODO() }

        fun isExternal(typeParameter: IrTypeParameter): Boolean { TODO() }

        fun addPotentiallyCapturedTypeParameter(param: IrTypeParameter) { TODO() }

        fun seeType(type: IrType) { TODO() }
    }

    private fun includeInParent(builder: ClosureBuilder) { TODO() }

    private val IrClass.closureBuilder: ClosureBuilder
        get() = closureBuilders.getOrPut(this) {
            val closureBuilder = ClosureBuilder(this)

            collectPotentiallyCapturedTypeParameters(closureBuilder)

            closureBuilder.declareVariable(this.thisReceiver)
            if (this.isInner) {
                val receiver = when (val parent = this.parent) {
                    is IrClass -> parent.thisReceiver
                    is IrScript -> parent.thisReceiver
                    else -> error("unexpected parent $parent")
                }
                closureBuilder.declareVariable(receiver)
                includeInParent(closureBuilder)
            }

            this.declarations.firstOrNull { it is IrConstructor && it.isPrimary }?.let {
                val constructor = it as IrConstructor
                constructor.nonDispatchParameters.forEach { v -> closureBuilder.declareVariable(v) }
            }

            closureBuilder
        }

    private val IrFunction.closureBuilder: ClosureBuilder
        get() = closureBuilders.getOrPut(this) {
            val closureBuilder = ClosureBuilder(this)

            collectPotentiallyCapturedTypeParameters(closureBuilder)

            this.parameters.forEach { closureBuilder.declareVariable(it) }
            closureBuilder.seeType(this.returnType)

            if (this is IrConstructor) {
                val constructedClass = (this.parent as IrClass)
                closureBuilder.declareVariable(constructedClass.thisReceiver)

                // Include closure of the class in the constructor closure.
                val classBuilder = constructedClass.closureBuilder
                closureBuilder.include(classBuilder)
            }

            closureBuilder
        }

    private fun collectPotentiallyCapturedTypeParameters(closureBuilder: ClosureBuilder) { TODO() }

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

    private inner class ClosureCollectorVisitor : IrVisitor<Unit, ClosureBuilder>() {

        override fun visitElement(element: IrElement, data: ClosureBuilder) { TODO() }

        override fun visitClass(declaration: IrClass, data: ClosureBuilder) { TODO() }

        override fun visitFunction(declaration: IrFunction, data: ClosureBuilder) { TODO() }

        override fun visitTypeParameter(declaration: IrTypeParameter, data: ClosureBuilder) { TODO() }

        override fun visitValueAccess(expression: IrValueAccessExpression, data: ClosureBuilder) { TODO() }

        override fun visitVariable(declaration: IrVariable, data: ClosureBuilder) { TODO() }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: ClosureBuilder) { TODO() }

        override fun visitFunctionReference(expression: IrFunctionReference, data: ClosureBuilder) { TODO() }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: ClosureBuilder) { TODO() }

        override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: ClosureBuilder) { TODO() }

        override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: ClosureBuilder) { TODO() }

        override fun visitPropertyReference(expression: IrPropertyReference, data: ClosureBuilder) { TODO() }

        override fun visitExpression(expression: IrExpression, data: ClosureBuilder) { TODO() }

        private fun processScriptCapturing(expression: IrFunctionAccessExpression, data: ClosureBuilder) { TODO() }

        private fun processMemberAccess(declaration: IrDeclaration, parentClosure: ClosureBuilder) { TODO() }
    }
}
