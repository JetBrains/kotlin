package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.backend.konan.lower.VarargInjectionLowering.Companion.kIntType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.*
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
                    var parameterIndex = 0
                    expression.acceptChildrenVoid(object:IrElementVisitorVoid{
                        override fun visitElement(element: IrElement) {
                            offset(expression, scope) {
                                val parameter = if (element is IrVararg && varargToBlock.containsKey(element))
                                    varargToBlock[element]!!
                                else element
                                originalCall.putValueArgument(parameterIndex++, parameter as IrExpression)
                            }
                        }
                    })
                    return originalCall
                }
            }
        })
    }


    private fun blockPerVararg(expression: IrCall, owner: FunctionDescriptor): Map<IrVararg, IrBlock> {
        val varargArgs = mutableMapOf<IrVararg, IrBlock>()
        val arrayConstructor = kArrayType.constructors.find { it.valueParameters.size == 1 }
        expression.acceptVoid(object : IrElementVisitorVoid {
            val scope = owner.scope()
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVararg(expression: IrVararg) {
                offset(expression, scope) {
                    val hasSpreadElement = hasSpreadElement(expression)
                    val block = irBlock(kArrayType.defaultType)
                    if (!hasSpreadElement && expression.elements.all { it is IrConst<*> && KotlinBuiltIns.isString(it.type)}) {
                        log("skipped vararg expression because it's string array literal")
                        return
                    }

                    val vars = expression.elements.map {
                        val initVar = scope.createTemporaryVariable((it as? IrSpreadElement)?.expression ?: it as IrExpression, "__elem\$", true)
                        block.statements.add(initVar)
                        it to initVar
                    }.toMap()
                    val arrayConstructorCall = irCall(
                            descriptor    = arrayConstructor!!,
                            typeArguments = mapOf(arrayConstructor.typeParameters[0] to expression.varargElementType))
                    arrayConstructorCall.putValueArgument(0,  calculateArraySize(hasSpreadElement, scope,  expression, vars))
                    val arrayTmpVariable = scope.createTemporaryVariable(arrayConstructorCall, "__array\$", true)
                    val indexTmpVariable = scope.createTemporaryVariable(kIntZero, "__index\$", true)
                    block.statements.add(arrayTmpVariable)
                    if (hasSpreadElement) {
                        block.statements.add(indexTmpVariable)
                    }
                    expression.elements.forEachIndexed { i, element ->
                        offset(expression, scope) {
                            log("element:$i> ${ir2string(element)}")
                            val dst = vars[element]!!
                            if (element !is IrSpreadElement) {
                                val setArrayElementCall = irCall(
                                        descriptor    = kArraySetFunctionDescriptor,
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
                                val arraySizeVariable = scope.createTemporaryVariable(irArraySize(irGet(dst.descriptor)), "__length\$")
                                block.statements.add(arraySizeVariable)
                                val copyCall = irCall(kCopyRangeToDescriptor, null).apply {
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
                    varargArgs.put(expression, block)
                }
            }
        })
        return varargArgs
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
    private fun calculateArraySize(hasSpreadElement: Boolean, scope:Scope, expression: IrVararg, vars: Map<IrVarargElement, IrVariable>): IrExpression? {
        offset(expression, scope) {
            if (!hasSpreadElement)
                return irConstInt(expression.elements.size)
            val notSpreadElementCount = expression.elements.filter { it !is IrSpreadElement}.size
            val initialValue = irConstInt(notSpreadElementCount) as IrExpression
            return vars.filter{it.key is IrSpreadElement}.toList().fold( initial = initialValue) { result, it ->
                val arraySize = irArraySize(irGet(it.second.descriptor))
                increment(result, arraySize)
            }
        }

    }

    private fun Offset.irArraySize(expression: IrExpression): IrExpression {
        val arraySize = irCall((kArraySizeFunctionDescriptor as PropertyDescriptor).getter as FunctionDescriptor, null).apply {
            dispatchReceiver = expression
        }
        return arraySize
    }


    private fun hasSpreadElement(expression: IrVararg) = expression.elements.any { it is IrSpreadElement }

    private fun log(msg:String) {
        context.log("VARARG-INJECTOR:    $msg")
    }

    companion object {
        val kArrayType                    = KonanPlatform.builtIns.array
        val kCollectionsPackageDescriptor = KonanPlatform.builtIns.builtInsModule.getPackage(FqName("kotlin.collections")).memberScope
        val kCopyRangeToDescriptor        = DescriptorUtils.getAllDescriptors(kCollectionsPackageDescriptor).filter {
                       it.name.asString() == "copyRangeTo"
                    && DescriptorUtils.getClassDescriptorForType((it as FunctionDescriptor).extensionReceiverParameter!!.type) == kArrayType}.first() as CallableDescriptor
        val unsubstitutedMemberScope      = kArrayType.unsubstitutedMemberScope
        val kArraySetFunctionDescriptor   = DescriptorUtils.getFunctionByName(unsubstitutedMemberScope, Name.identifier("set"))
        val kArraySizeFunctionDescriptor  = DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope).find { it.name.asString() == "size" }
        val kInt                          =  KonanPlatform.builtIns.int
        val kIntType                      =  KonanPlatform.builtIns.intType
        val kIntPlusDescriptor            = DescriptorUtils.getAllDescriptors(kInt.unsubstitutedMemberScope).find {
                        it.name.asString() == "plus"
                    && (it as FunctionDescriptor).valueParameters[0].type == kIntType} as FunctionDescriptor
    }
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

private fun Offset.irConstInt(value: Int): IrConst<Int> = irConst<Int>(kind = IrConstKind.Int, value = value, type = kIntType)
private fun  <T> Offset.irConst(kind: IrConstKind<T>, value: T, type: KotlinType): IrConst<T> = IrConstImpl<T>(startOffset, endOffset,type, kind,value)
private fun Offset.irBlock(type: KotlinType): IrBlock = IrBlockImpl(startOffset, endOffset, type)
private fun Offset.irCall(descriptor: CallableDescriptor, typeArguments: Map<TypeParameterDescriptor, KotlinType>?): IrCall = IrCallImpl(startOffset, endOffset, descriptor, typeArguments)
inline internal fun <R> offset(context:IrGeneratorContext, scope: Scope, startOffset: Int, endOffset: Int, block: Offset.() -> R):R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return Offset.use(context, scope, startOffset, endOffset, block)
}
