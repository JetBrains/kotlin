package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

internal class DataClassOperatorsLowering(val context: Context): FunctionLoweringPass {

    private val irBuiltins = context.irModule!!.irBuiltins

    override fun lower(irFunction: IrFunction) {
        irFunction.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol != irBuiltins.dataClassArrayMemberToStringSymbol
                        && expression.symbol != irBuiltins.dataClassArrayMemberHashCodeSymbol)
                    return expression

                val argument = expression.getValueArgument(0)!!
                val argumentType = argument.type.makeNotNullable()
                val genericType =
                        if (argumentType.arguments.isEmpty())
                            argumentType
                        else
                            (argumentType.constructor.declarationDescriptor as ClassDescriptor).defaultType
                val isToString = expression.symbol == irBuiltins.dataClassArrayMemberToStringSymbol
                val newSymbol = if (isToString)
                                    context.ir.symbols.arrayContentToString[genericType]!!
                                else
                                    context.ir.symbols.arrayContentHashCode[genericType]!!

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(irFunction.symbol, startOffset, endOffset)

                return irBuilder.run {
                    val typeArguments =
                            if (argumentType.arguments.isEmpty())
                                emptyList<KotlinType>()
                            else argumentType.arguments.map { it.type }
                    if (!argument.type.isMarkedNullable) {
                        irCall(newSymbol, typeArguments).apply {
                            extensionReceiver = argument
                        }
                    } else {
                        val tmp = scope.createTemporaryVariable(argument)
                        val call = irCall(newSymbol, typeArguments).apply {
                            extensionReceiver = irGet(tmp.symbol)
                        }
                        irBlock(argument) {
                            +tmp
                            +irIfThenElse(call.type,
                                    irEqeqeq(irGet(tmp.symbol), irNull()),
                                    if (isToString)
                                        irString("null")
                                    else
                                        irInt(0),
                                    call)
                        }
                    }
                }
            }
        })
    }
}