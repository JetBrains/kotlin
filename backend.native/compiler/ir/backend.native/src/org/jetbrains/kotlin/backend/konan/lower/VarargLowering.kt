package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType


class VarargInjectionLowering internal constructor(val context: Context): FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        if (irFunction.body != null)
            lower(irFunction.descriptor, irFunction.body!!)
    }

    private fun lower(owner:FunctionDescriptor, irBody: IrBody) {
        irBody.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                context.createIrBuilder(owner, expression.startOffset, expression.endOffset).apply {
                    expression.descriptor.valueParameters.filter{it.varargElementType != null && expression.getValueArgument(it) == null}.forEach {
                        expression.putValueArgument(it.index,
                            IrVarargImpl(startOffset       = startOffset,
                                         endOffset         = endOffset,
                                         type              = it.type,
                                         varargElementType = it.varargElementType!!)
                        )
                    }
                }
                super.visitCall(expression)
                return expression
            }

            override fun visitVararg(expression: IrVararg): IrExpression {
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
                                setArrayElementCall.dispatchReceiver = irGet(arrayTmpVariable.descriptor)
                                setArrayElementCall.putValueArgument(0, if (hasSpreadElement) irGet(indexTmpVariable.descriptor) else irConstInt(i))
                                setArrayElementCall.putValueArgument(1, irGet(dst.descriptor))
                                block.statements.add(setArrayElementCall)
                                if (hasSpreadElement) {
                                    block.statements.add(incrementVariable(indexTmpVariable.descriptor, kIntOne))
                                }
                            } else {
                                val arraySizeVariable = scope.createTemporaryVariable(irArraySize(arrayHandle, irGet(dst.descriptor)), "length".synthesizedString)
                                block.statements.add(arraySizeVariable)
                                val copyCall = irCall(arrayHandle.copyRangeToDescriptor, null).apply {
                                    extensionReceiver = irGet(dst.descriptor)
                                    putValueArgument(0, irGet(arrayTmpVariable.descriptor))  /* destination */
                                    putValueArgument(1, kIntZero)                            /* fromIndex */
                                    putValueArgument(2, irGet(arraySizeVariable.descriptor)) /* toIndex */
                                    putValueArgument(3, irGet(indexTmpVariable.descriptor))  /* destinationIndex */
                                }
                                block.statements.add(copyCall)
                                block.statements.add(incrementVariable(indexTmpVariable.descriptor,
                                        irGet(arraySizeVariable.descriptor)))
                                log("element:$i:spread element> ${ir2string(element.expression)}")
                            }
                        }
                    }
                    block.statements.add(irGet(arrayTmpVariable.descriptor))
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

    private fun IrBuilderWithScope.incrementVariable(descriptor: VariableDescriptor, value: IrExpression): IrExpression {
        return irSetVar(descriptor, intPlus().apply {
            dispatchReceiver = irGet(descriptor)
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
                val arraySize = irArraySize(arrayHandle, irGet(it.second.descriptor))
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
        context.log("VARARG-INJECTOR:    $msg")
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

    private fun copyRangeToFunctionDescriptor(descriptor:ClassDescriptor):FunctionDescriptor {
        val packageViewDescriptor = KonanPlatform.builtIns.builtInsModule.getPackage(FqName("kotlin.collections"))
        return packageViewDescriptor.memberScope.getContributedFunctions(Name.identifier("copyRangeTo"), NoLookupLocation.FROM_BACKEND).filter {
            it.extensionReceiverParameter != null && DescriptorUtils.getClassDescriptorForType(it.extensionReceiverParameter!!.type) == descriptor
        }.first()
    }
    private fun setMethodDescriptor(descriptor:ClassDescriptor) = methodDescriptor(descriptor, "set")
    private fun sizeMethodDescriptor(descriptor:ClassDescriptor) = descriptor(descriptor, "size")
    private fun methodDescriptor(descriptor:ClassDescriptor, methodName:String) = DescriptorUtils.getFunctionByName(descriptor.unsubstitutedMemberScope, Name.identifier(methodName))
    private fun arrayClassDescriptor(name:String) = kKotlinPackage.memberScope.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    private fun descriptor(descriptor: ClassDescriptor, name:String) = DescriptorUtils.getAllDescriptors(descriptor.unsubstitutedMemberScope).find{it.name.asString() == name}
}

private fun IrBuilderWithScope.irConstInt(value: Int): IrConst<Int> = IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, value)
private fun IrBuilderWithScope.irBlock(type: KotlinType): IrBlock = IrBlockImpl(startOffset, endOffset, type)
private fun IrBuilderWithScope.irCall(descriptor: CallableDescriptor, typeArguments: Map<TypeParameterDescriptor, KotlinType>?): IrCall = IrCallImpl(startOffset, endOffset, descriptor, typeArguments)
private val IrBuilderWithScope.kIntZero get() = irConstInt(0)
private val IrBuilderWithScope.kIntOne get() = irConstInt(1)
