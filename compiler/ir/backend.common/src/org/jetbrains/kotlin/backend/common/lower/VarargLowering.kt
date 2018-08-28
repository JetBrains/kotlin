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

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions


class VarargInjectionLowering constructor(val context: CommonBackendContext): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach{
            when (it) {
                is IrField    -> lower(it.symbol, it.initializer)
                is IrFunction -> lower(it.symbol, it.body)
                is IrProperty -> {
                    it.backingField?.let { field ->
                        lower(field.symbol, field)
                    }
                    it.getter?.let { getter ->
                        lower(getter.symbol, getter)
                    }
                    it.setter?.let { setter ->
                        lower(setter.symbol, setter)
                    }
                }
            }
        }
    }

    private fun lower(owner: IrSymbol, element: IrElement?) {
        element?.transformChildrenVoid(object: IrElementTransformerVoid() {
            val transformer = this

            private fun replaceEmptyParameterWithEmptyArray(expression: IrFunctionAccessExpression) {
                log { "call of: ${expression.descriptor}" }
                context.createIrBuilder(owner, expression.startOffset, expression.endOffset).apply {
                    expression.descriptor.valueParameters.forEach {
                        log { "varargElementType: ${it.varargElementType} expr: ${ir2string(expression.getValueArgument(it))}" }
                    }
                    expression.symbol.owner.valueParameters
                        .filter { it.varargElementType != null && expression.getValueArgument(it.index) == null }
                        .forEach {
                            expression.putValueArgument(
                                it.index,
                                IrVarargImpl(
                                    startOffset = startOffset,
                                    endOffset = endOffset,
                                    type = it.type,
                                    varargElementType = it.varargElementType!!
                                )
                            )
                        }
                }
                expression.transformChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                replaceEmptyParameterWithEmptyArray(expression)
                return expression
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                replaceEmptyParameterWithEmptyArray(expression)
                return expression
            }

            override fun visitVararg(expression: IrVararg): IrExpression {
                expression.transformChildrenVoid(transformer)
                val hasSpreadElement = hasSpreadElement(expression)
                if (!hasSpreadElement && expression.elements.all { it is IrConst<*> && it.type.isString() }) {
                    log { "skipped vararg expression because it's string array literal" }
                    return expression
                }
                val irBuilder = context.createIrBuilder(owner, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    val type = expression.varargElementType
                    log { "$expression: array type:$type, is array of primitives ${!expression.type.isArray()}" }
                    val arrayHandle = arrayType(expression.type)
                    val block = irBlock(expression.type)

                    val vars = expression.elements.map {
                        val initVar = scope.createTemporaryVariable(
                            (it as? IrSpreadElement)?.expression ?: it as IrExpression,
                            "elem".synthesizedString, true)
                        block.statements.add(initVar)
                        it to initVar
                    }.toMap()
                    val arraySize = calculateArraySize(arrayHandle, hasSpreadElement, scope, expression, vars)
                    val array = arrayHandle.createArray(this, expression.varargElementType, arraySize)

                    val arrayTmpVariable = scope.createTemporaryVariable(array, "array".synthesizedString, true)
                    val indexTmpVariable = scope.createTemporaryVariable(kIntZero, "index".synthesizedString, true)
                    block.statements.add(arrayTmpVariable)
                    if (hasSpreadElement) {
                        block.statements.add(indexTmpVariable)
                    }
                    expression.elements.forEachIndexed { i, element ->
                        irBuilder.startOffset = element.startOffset
                        irBuilder.endOffset   = element.endOffset
                        irBuilder.apply {
                            log { "element:$i> ${ir2string(element)}" }
                            val dst = vars[element]!!
                            if (element !is IrSpreadElement) {
                                val setArrayElementCall = irCall(arrayHandle.setMethodSymbol.owner)
                                setArrayElementCall.dispatchReceiver = irGet(arrayTmpVariable)
                                setArrayElementCall.putValueArgument(0, if (hasSpreadElement) irGet(indexTmpVariable) else irConstInt(i))
                                setArrayElementCall.putValueArgument(1, irGet(dst))
                                block.statements.add(setArrayElementCall)
                                if (hasSpreadElement) {
                                    block.statements.add(incrementVariable(indexTmpVariable, kIntOne))
                                }
                            } else {
                                val arraySizeVariable = scope.createTemporaryVariable(irArraySize(arrayHandle, irGet(dst)), "length".synthesizedString)
                                block.statements.add(arraySizeVariable)
                                val copyCall = irCall(arrayHandle.copyRangeToSymbol.owner).apply {
                                    extensionReceiver = irGet(dst)
                                    putValueArgument(0, irGet(arrayTmpVariable))  /* destination */
                                    putValueArgument(1, kIntZero)                            /* fromIndex */
                                    putValueArgument(2, irGet(arraySizeVariable)) /* toIndex */
                                    putValueArgument(3, irGet(indexTmpVariable))  /* destinationIndex */
                                }
                                block.statements.add(copyCall)
                                block.statements.add(incrementVariable(indexTmpVariable, irGet(arraySizeVariable)))
                                log { "element:$i:spread element> ${ir2string(element.expression)}" }
                            }
                        }
                    }
                    block.statements.add(irGet(arrayTmpVariable))
                    return block
                }
            }
        })
    }

    private val symbols = context.ir.symbols
    private val intPlusInt = symbols.intPlusInt.owner

    private fun arrayType(type: IrType): ArrayHandle {
        val arrayClass = type.classifierOrFail
        return arrayToHandle[arrayClass] ?: error(arrayClass.descriptor)
    }

    private fun IrBuilderWithScope.intPlus() = irCall(intPlusInt)
    private fun IrBuilderWithScope.increment(expression: IrExpression, value: IrExpression): IrExpression {
        return intPlus().apply {
            dispatchReceiver = expression
            putValueArgument(0, value)
        }
    }

    private fun IrBuilderWithScope.incrementVariable(variable: IrVariable, value: IrExpression): IrExpression {
        return irSetVar(variable.symbol, intPlus().apply {
            dispatchReceiver = irGet(variable)
            putValueArgument(0, value)
        })
    }
    private fun calculateArraySize(arrayHandle: ArrayHandle, hasSpreadElement: Boolean, scope:Scope, expression: IrVararg, vars: Map<IrVarargElement, IrVariable>): IrExpression {
        context.createIrBuilder(scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            if (!hasSpreadElement)
                return irConstInt(expression.elements.size)
            val notSpreadElementCount = expression.elements.filter { it !is IrSpreadElement}.size
            val initialValue = irConstInt(notSpreadElementCount) as IrExpression
            return vars.filter{it.key is IrSpreadElement}.toList().fold( initial = initialValue) { result, it ->
                val arraySize = irArraySize(arrayHandle, irGet(it.second))
                increment(result, arraySize)
            }
        }
    }

    private fun IrBuilderWithScope.irArraySize(arrayHandle: ArrayHandle, expression: IrExpression): IrExpression {
        val arraySize = irCall(arrayHandle.sizeGetterSymbol.owner).apply {
            dispatchReceiver = expression
        }
        return arraySize
    }


    private fun hasSpreadElement(expression: IrVararg?) = expression?.elements?.any { it is IrSpreadElement }?:false

    private fun log(msg:() -> String) {
        context.log { "VARARG-INJECTOR:    ${msg()}" }
    }

    abstract inner class ArrayHandle(val arraySymbol: IrClassSymbol) {
        val setMethodSymbol = arraySymbol.functions.single { it.descriptor.name == OperatorNameConventions.SET }
        val sizeGetterSymbol = arraySymbol.getPropertyGetter("size")!!
        val copyRangeToSymbol = symbols.copyRangeTo[arraySymbol.descriptor]!!
        protected val singleParameterConstructor =
            arraySymbol.owner.constructors.find { it.valueParameters.size == 1 }!!

        abstract fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression
    }

    inner class ReferenceArrayHandle : ArrayHandle(symbols.array) {
        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            return builder.irCall(singleParameterConstructor).apply {
                putTypeArgument(0, elementType)
                putValueArgument(0, size)
            }
        }
    }

    inner class PrimitiveArrayHandle(primitiveType: PrimitiveType)
        : ArrayHandle(symbols.primitiveArrays[primitiveType]!!) {

        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            return builder.irCall(singleParameterConstructor).apply {
                putValueArgument(0, size)
            }
        }
    }

    inner class UnsignedArrayHandle(
        arraySymbol: IrClassSymbol,
        private val wrappedArrayHandle: PrimitiveArrayHandle
    ) : ArrayHandle(arraySymbol) {

        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            val wrappedArray = wrappedArrayHandle.createArray(builder, elementType, size)
            return builder.irCall(singleParameterConstructor).apply {
                putValueArgument(0, wrappedArray)
            }
        }
    }

    private val primitiveArrayHandles = PrimitiveType.values().associate { it to PrimitiveArrayHandle(it) }

    private val unsignedArrayHandles = UnsignedType.values().mapNotNull { unsignedType ->
        symbols.unsignedArrays[unsignedType]?.let {
            val primitiveType = when (unsignedType) {
                UnsignedType.UBYTE -> PrimitiveType.BYTE
                UnsignedType.USHORT -> PrimitiveType.SHORT
                UnsignedType.UINT -> PrimitiveType.INT
                UnsignedType.ULONG -> PrimitiveType.LONG
            }
            UnsignedArrayHandle(it, primitiveArrayHandles[primitiveType]!!)
        }
    }

    val arrayToHandle =
        (primitiveArrayHandles.values + unsignedArrayHandles + ReferenceArrayHandle()).associateBy { it.arraySymbol }

}

private fun IrBuilderWithScope.irConstInt(value: Int): IrConst<Int> =
    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, value)
private fun IrBuilderWithScope.irBlock(type: IrType): IrBlock = IrBlockImpl(startOffset, endOffset, type)
private val IrBuilderWithScope.kIntZero get() = irConstInt(0)
private val IrBuilderWithScope.kIntOne get() = irConstInt(1)
