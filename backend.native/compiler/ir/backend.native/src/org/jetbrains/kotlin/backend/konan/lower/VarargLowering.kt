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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType


class VarargInjectionLowering internal constructor(val context: Context): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach{
            when (it) {
                is IrField    -> lower(it.descriptor, it.initializer)
                is IrFunction -> lower(it.descriptor, it.body)
                is IrProperty -> {
                    lower(it.descriptor, it.backingField)
                    if (it.getter != null)
                        lower(it.getter!!.descriptor, it.getter)
                    if (it.setter != null)
                        lower(it.setter!!.descriptor, it.setter)
                }
            }
        }
    }

    private fun lower(owner:DeclarationDescriptor, element: IrElement?) {
        element?.transformChildrenVoid(object: IrElementTransformerVoid() {
            val transformer = this

            private fun replaceEmptyParameterWithEmptyArray(expression: IrMemberAccessExpression) {
                log("call of: ${expression.descriptor}")
                context.createIrBuilder(owner, expression.startOffset, expression.endOffset).apply {
                    expression.descriptor.valueParameters.forEach {
                        log("varargElementType: ${it.varargElementType} expr: ${ir2string(expression.getValueArgument(it))}")
                    }
                    expression.descriptor.valueParameters.filter { it.varargElementType != null && expression.getValueArgument(it) == null }.forEach {
                        expression.putValueArgument(it.index,
                                IrVarargImpl(startOffset       = startOffset,
                                             endOffset         = endOffset,
                                             type              = it.type,
                                             varargElementType = it.varargElementType!!)
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
                if (!hasSpreadElement && expression.elements.all { it is IrConst<*> && KotlinBuiltIns.isString(it.type) }) {
                    log("skipped vararg expression because it's string array literal")
                    return expression
                }
                val irBuilder = context.createIrBuilder(owner, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    val type = expression.varargElementType
                    log("$expression: array type:$type, is array of primitives ${!KotlinBuiltIns.isArray(expression.type)}")
                    val arrayHandle = arrayType(expression.type)
                    val arrayConstructor = arrayHandle.arrayDescriptor.constructors.find { it.valueParameters.size == 1 }!!
                    val block = irBlock(arrayHandle.arrayDescriptor.defaultType)
                    val arrayConstructorCall = irCall(
                            descriptor = arrayConstructor,
                            typeArguments = typeArgument(arrayConstructor, type))


                    val vars = expression.elements.map {
                        val initVar = scope.createTemporaryVariable(
                                (it as? IrSpreadElement)?.expression ?: it as IrExpression,
                                "elem".synthesizedString, true)
                        block.statements.add(initVar)
                        it to initVar
                    }.toMap()
                    arrayConstructorCall.putValueArgument(0, calculateArraySize(arrayHandle, hasSpreadElement, scope, expression, vars))
                    val arrayTmpVariable = scope.createTemporaryVariable(arrayConstructorCall, "array".synthesizedString, true)
                    val indexTmpVariable = scope.createTemporaryVariable(kIntZero, "index".synthesizedString, true)
                    block.statements.add(arrayTmpVariable)
                    if (hasSpreadElement) {
                        block.statements.add(indexTmpVariable)
                    }
                    expression.elements.forEachIndexed { i, element ->
                        irBuilder.startOffset = element.startOffset
                        irBuilder.endOffset   = element.endOffset
                        irBuilder.apply {
                            log("element:$i> ${ir2string(element)}")
                            val dst = vars[element]!!
                            if (element !is IrSpreadElement) {
                                val setArrayElementCall = irCall(
                                        descriptor    = arrayHandle.setMethodDescriptor,
                                        typeArguments = null
                                )
                                setArrayElementCall.dispatchReceiver = irGet(arrayTmpVariable.symbol)
                                setArrayElementCall.putValueArgument(0, if (hasSpreadElement) irGet(indexTmpVariable.symbol) else irConstInt(i))
                                setArrayElementCall.putValueArgument(1, irGet(dst.symbol))
                                block.statements.add(setArrayElementCall)
                                if (hasSpreadElement) {
                                    block.statements.add(incrementVariable(indexTmpVariable.symbol, kIntOne))
                                }
                            } else {
                                val arraySizeVariable = scope.createTemporaryVariable(irArraySize(arrayHandle, irGet(dst.symbol)), "length".synthesizedString)
                                block.statements.add(arraySizeVariable)
                                val copyCall = irCall(arrayHandle.copyRangeToDescriptor, null).apply {
                                    extensionReceiver = irGet(dst.symbol)
                                    putValueArgument(0, irGet(arrayTmpVariable.symbol))  /* destination */
                                    putValueArgument(1, kIntZero)                            /* fromIndex */
                                    putValueArgument(2, irGet(arraySizeVariable.symbol)) /* toIndex */
                                    putValueArgument(3, irGet(indexTmpVariable.symbol))  /* destinationIndex */
                                }
                                block.statements.add(copyCall)
                                block.statements.add(incrementVariable(indexTmpVariable.symbol,
                                        irGet(arraySizeVariable.symbol)))
                                log("element:$i:spread element> ${ir2string(element.expression)}")
                            }
                        }
                    }
                    block.statements.add(irGet(arrayTmpVariable.symbol))
                    return block
                }
            }
        })
    }


    private fun typeArgument(arrayConstructor: ClassConstructorDescriptor, type: KotlinType):Map<TypeParameterDescriptor, KotlinType>? {
        return if (!arrayConstructor.typeParameters.isEmpty())
            mapOf(arrayConstructor.typeParameters.first() to type)
        else
            null
    }

    private fun arrayType(type: KotlinType): ArrayHandle = when {
        KotlinBuiltIns.isPrimitiveArray(type) -> {
            val primitiveType = KotlinBuiltIns.getPrimitiveTypeByArrayClassFqName(DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!))
            when (primitiveType) {
                PrimitiveType.BYTE    -> kByteArrayHandler
                PrimitiveType.SHORT   -> kShortArrayHandler
                PrimitiveType.CHAR    -> kCharArrayHandler
                PrimitiveType.INT     -> kIntArrayHandler
                PrimitiveType.LONG    -> kLongArrayHandler
                PrimitiveType.FLOAT   -> kFloatArrayHandler
                PrimitiveType.DOUBLE  -> kDoubleArrayHandler
                PrimitiveType.BOOLEAN -> kBooleanArrayHandler
                else                  -> TODO("unsupported type: $primitiveType")
            }
        }
        else -> kArrayHandler
    }

    private fun IrBuilderWithScope.intPlus() = irCall(kIntPlusDescriptor, null)
    private fun IrBuilderWithScope.increment(expression: IrExpression, value: IrExpression): IrExpression {
        return intPlus().apply {
            dispatchReceiver = expression
            putValueArgument(0, value)
        }
    }

    private fun IrBuilderWithScope.incrementVariable(symbol: IrVariableSymbol, value: IrExpression): IrExpression {
        return irSetVar(symbol, intPlus().apply {
            dispatchReceiver = irGet(symbol)
            putValueArgument(0, value)
        })
    }
    private fun calculateArraySize(arrayHandle: ArrayHandle, hasSpreadElement: Boolean, scope:Scope, expression: IrVararg, vars: Map<IrVarargElement, IrVariable>): IrExpression? {
        context.createIrBuilder(scope.scopeOwner, expression.startOffset, expression.endOffset).run {
            if (!hasSpreadElement)
                return irConstInt(expression.elements.size)
            val notSpreadElementCount = expression.elements.filter { it !is IrSpreadElement}.size
            val initialValue = irConstInt(notSpreadElementCount) as IrExpression
            return vars.filter{it.key is IrSpreadElement}.toList().fold( initial = initialValue) { result, it ->
                val arraySize = irArraySize(arrayHandle, irGet(it.second.symbol))
                increment(result, arraySize)
            }
        }
    }

    private fun IrBuilderWithScope.irArraySize(arrayHandle: ArrayHandle, expression: IrExpression): IrExpression {
        val arraySize = irCall((arrayHandle.sizeDescriptor as PropertyDescriptor).getter as FunctionDescriptor, null).apply {
            dispatchReceiver = expression
        }
        return arraySize
    }


    private fun hasSpreadElement(expression: IrVararg?) = expression?.elements?.any { it is IrSpreadElement }?:false

    private fun log(msg:String) {
        context.log{"VARARG-INJECTOR:    $msg"}
    }

    data class ArrayHandle(val arrayDescriptor:ClassDescriptor,
                           val setMethodDescriptor: FunctionDescriptor,
                           val sizeDescriptor:DeclarationDescriptor,
                           val copyRangeToDescriptor:FunctionDescriptor)
    val kKotlinPackage       = context.builtIns.builtInsModule.getPackage(FqName("kotlin"))
    val kByteArrayHandler    = handle(arrayClassDescriptor("ByteArray"))
    val kCharArrayHandler    = handle(arrayClassDescriptor("CharArray"))
    val kShortArrayHandler   = handle(arrayClassDescriptor("ShortArray"))
    val kIntArrayHandler     = handle(arrayClassDescriptor("IntArray"))
    val kLongArrayHandler    = handle(arrayClassDescriptor("LongArray"))
    val kFloatArrayHandler   = handle(arrayClassDescriptor("FloatArray"))
    val kDoubleArrayHandler  = handle(arrayClassDescriptor("DoubleArray"))
    val kBooleanArrayHandler = handle(arrayClassDescriptor("BooleanArray"))
    val kArrayHandler        = handle(context.builtIns.array)

    val kInt               = context.builtIns.int
    val kIntType           =  context.builtIns.intType
    val kIntPlusDescriptor = DescriptorUtils.getAllDescriptors(kInt.unsubstitutedMemberScope).find {
                        it.name.asString() == "plus"
                    && (it as FunctionDescriptor).valueParameters[0].type == kIntType} as FunctionDescriptor

    private fun handle(descriptor:ClassDescriptor) = ArrayHandle(
            arrayDescriptor       = descriptor,
            setMethodDescriptor   = setMethodDescriptor(descriptor),
            sizeDescriptor        = sizeMethodDescriptor(descriptor)!!,
            copyRangeToDescriptor = copyRangeToFunctionDescriptor(descriptor))

    private fun copyRangeToFunctionDescriptor(descriptor: ClassDescriptor): FunctionDescriptor {
        val packageViewDescriptor = descriptor.module.getPackage(KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME)
        return packageViewDescriptor.memberScope.getContributedFunctions(Name.identifier("copyRangeTo"), NoLookupLocation.FROM_BACKEND).first {
            it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == descriptor
        }
    }
    private fun setMethodDescriptor(descriptor:ClassDescriptor) = methodDescriptor(descriptor, "set")
    private fun sizeMethodDescriptor(descriptor:ClassDescriptor) = descriptor(descriptor, "size")
    private fun methodDescriptor(descriptor:ClassDescriptor, methodName:String) = DescriptorUtils.getFunctionByName(descriptor.unsubstitutedMemberScope, Name.identifier(methodName))
    private fun arrayClassDescriptor(name:String) = kKotlinPackage.memberScope.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    private fun descriptor(descriptor: ClassDescriptor, name:String) = DescriptorUtils.getAllDescriptors(descriptor.unsubstitutedMemberScope).find{it.name.asString() == name}
}

private fun IrBuilderWithScope.irConstInt(value: Int): IrConst<Int> = IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, value)
private fun IrBuilderWithScope.irBlock(type: KotlinType): IrBlock = IrBlockImpl(startOffset, endOffset, type)
private fun IrBuilderWithScope.irCall(descriptor: FunctionDescriptor, typeArguments: Map<TypeParameterDescriptor, KotlinType>?): IrCall = IrCallImpl(startOffset, endOffset, descriptor, typeArguments)
private val IrBuilderWithScope.kIntZero get() = irConstInt(0)
private val IrBuilderWithScope.kIntOne get() = irConstInt(1)
