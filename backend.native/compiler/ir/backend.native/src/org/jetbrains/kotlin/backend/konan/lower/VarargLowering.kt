package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
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

    fun noVarargArguments(descriptor: FunctionDescriptor) = descriptor.valueParameters.none { it.varargElementType != null }
    val irContext = IrGeneratorContext(context.ir.irModule.irBuiltins)

    private fun lower(owner:FunctionDescriptor, irBody: IrBody) {
        irBody.transformChildrenVoid(object: IrElementTransformerVoid(){
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val functionDescriptor = expression.descriptor as FunctionDescriptor
                if (noVarargArguments(functionDescriptor))
                    return expression
                val varargToBlock = blockPerVararg(expression, owner)
                val scope = owner.scope()
                offset(expression, scope){
                    val originalCall = irCall(
                            descriptor = expression.descriptor,
                            typeArguments = expression.descriptor.original.typeParameters.map { it to expression.getTypeArgument(it)!! }.toMap())
                    originalCall.dispatchReceiver  = expression.dispatchReceiver
                    originalCall.extensionReceiver = expression.extensionReceiver
                    functionDescriptor.valueParameters.forEach {
                        originalCall.putValueArgument(it.index, if (varargToBlock.containsKey(it)) varargToBlock[it] else expression.getValueArgument(it))
                    }
                    return originalCall
                }
            }
        })
    }


    private fun blockPerVararg(expression: IrCall, owner: FunctionDescriptor): Map<ValueParameterDescriptor, IrBlock> {
        val varargArgs = mutableMapOf<ValueParameterDescriptor, IrBlock>()
        val scope = owner.scope()
        val calleeDescriptor = expression.descriptor
        calleeDescriptor.valueParameters
                .filter{ it.varargElementType != null &&  expression.getValueArgument(it) is IrVararg?}
                .forEach {
            val type = it.varargElementType!!
            val parameterExpression = expression.getValueArgument(it) as IrVararg?
            offset(expression, scope) {
                val hasSpreadElement = hasSpreadElement(parameterExpression)
                if (!hasSpreadElement && parameterExpression?.elements?.all { it is IrConst<*> && KotlinBuiltIns.isString(it.type)}?:false) {
                    log("skipped vararg expression because it's string array literal")
                    return@forEach
                }
                val arrayHandle  = arrayType(type)
                val arrayConstructor = arrayHandle.arrayDescriptor.constructors.find { it.valueParameters.size == 1 }!!
                val block            = irBlock(arrayHandle.arrayDescriptor.defaultType)
                val arrayConstructorCall = irCall(
                        descriptor    = arrayConstructor,
                        typeArguments = typeArgument(arrayConstructor, type))

                if (parameterExpression == null) {
                    arrayConstructorCall.putValueArgument(0, kIntZero)
                    block.statements.add(arrayConstructorCall)
                    varargArgs.put(it, block)
                    return@forEach
                }

                val vars = parameterExpression.elements.map {
                    val initVar = scope.createTemporaryVariable((it as? IrSpreadElement)?.expression ?: it as IrExpression, "__elem\$", true)
                    block.statements.add(initVar)
                    it to initVar
                }.toMap()
                arrayConstructorCall.putValueArgument(0,  calculateArraySize(arrayHandle, hasSpreadElement, scope, parameterExpression, vars))
                val arrayTmpVariable = scope.createTemporaryVariable(arrayConstructorCall, "__array\$", true)
                val indexTmpVariable = scope.createTemporaryVariable(kIntZero, "__index\$", true)
                block.statements.add(arrayTmpVariable)
                if (hasSpreadElement) {
                    block.statements.add(indexTmpVariable)
                }
                parameterExpression.elements.forEachIndexed { i, element ->
                    offset(parameterExpression, scope) {
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
                            val arraySizeVariable = scope.createTemporaryVariable(irArraySize(arrayHandle, irGet(dst.descriptor)), "__length\$")
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
                varargArgs.put(it, block)
            }
        }
        return varargArgs
    }

    private fun typeArgument(arrayConstructor: ClassConstructorDescriptor, type: KotlinType):Map<TypeParameterDescriptor, KotlinType>? {
        return if (!arrayConstructor.typeParameters.isEmpty())
            mapOf(arrayConstructor.typeParameters.first() to type)
        else
            null
    }

    private fun arrayType(type: KotlinType): ArrayHandle {
        return when {
            KotlinBuiltIns.isByte(type)    -> kByteArrayHandler
            KotlinBuiltIns.isShort(type)   -> kShortArrayHandler
            KotlinBuiltIns.isChar(type)    -> kCharArrayHandler
            KotlinBuiltIns.isInt(type)     -> kIntArrayHandler
            KotlinBuiltIns.isLong(type)    -> kLongArrayHandler
            KotlinBuiltIns.isFloat(type)   -> kFloatArrayHandler
            KotlinBuiltIns.isDouble(type)  -> kDoubleArrayHandler
            KotlinBuiltIns.isBoolean(type) -> kBooleanArrayHandler
            else                           -> kArrayHandler
        }
    }

    private fun Offset.intPlus() = irCall(kIntPlusDescriptor, null)
    private fun Offset.increment(expression: IrExpression, value: IrExpression): IrExpression {
        return intPlus().apply {
            dispatchReceiver = expression
            putValueArgument(0, value)
        }
    }

    private fun Offset.incrementVariable(descriptor: VariableDescriptor, value: IrExpression): IrExpression {
        return irSetVar(descriptor, intPlus().apply {
            dispatchReceiver = irGet(descriptor)
            putValueArgument(0, value)
        })
    }
    private fun calculateArraySize(arrayHandle: ArrayHandle, hasSpreadElement: Boolean, scope:Scope, expression: IrVararg, vars: Map<IrVarargElement, IrVariable>): IrExpression? {
        offset(expression, scope) {
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

    private fun Offset.irArraySize(arrayHandle: ArrayHandle, expression: IrExpression): IrExpression {
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
    inline internal fun <R> offset(ir:IrElement, scope:Scope, block: Offset.() -> R):R = offset(irContext, scope, ir.startOffset, ir.endOffset, block)
}
internal class Offset(context:IrGeneratorContext, scope: Scope, startOffset:Int, endOfset:Int) : IrBuilderWithScope(context, scope, startOffset, endOfset) {
    val kIntZero = irConstInt(0)
    val kIntOne  = irConstInt(1)
    companion object {
        internal inline fun <R> use(context:IrGeneratorContext, scope: Scope, startOffset: Int, endOfset: Int, block: Offset.() -> R):R = Offset(context, scope, startOffset, endOfset).block()
    }
}

internal fun CallableDescriptor.scope() = Scope(this)

private fun Offset.irConstInt(value: Int): IrConst<Int> = irConst<Int>(kind = IrConstKind.Int, value = value, type = KonanPlatform.builtIns.intType)
private fun  <T> Offset.irConst(kind: IrConstKind<T>, value: T, type: KotlinType): IrConst<T> = IrConstImpl<T>(startOffset, endOffset,type, kind,value)
private fun Offset.irBlock(type: KotlinType): IrBlock = IrBlockImpl(startOffset, endOffset, type)
private fun Offset.irCall(descriptor: CallableDescriptor, typeArguments: Map<TypeParameterDescriptor, KotlinType>?): IrCall = IrCallImpl(startOffset, endOffset, descriptor, typeArguments)
inline internal fun <R> offset(context:IrGeneratorContext, scope: Scope, startOffset: Int, endOffset: Int, block: Offset.() -> R):R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return Offset.use(context, scope, startOffset, endOffset, block)
}
