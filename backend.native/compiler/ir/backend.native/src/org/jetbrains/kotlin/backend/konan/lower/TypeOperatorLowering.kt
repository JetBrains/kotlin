package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

/**
 * This lowering pass lowers some [IrTypeOperatorCall]s.
 */
internal class TypeOperatorLowering(val context: Context) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val transformer = TypeOperatorTransformer(context, irFunction.descriptor)
        irFunction.transformChildrenVoid(transformer)
    }
}


private class TypeOperatorTransformer(val context: Context, val function: FunctionDescriptor) : IrElementTransformerVoid() {

    private val builder = context.createFunctionIrBuilder(function)

    override fun visitFunction(declaration: IrFunction): IrStatement {
        // ignore inner functions during this pass
        return declaration
    }

    private tailrec fun KotlinType.erasure(): KotlinType {
        val descriptor = this.constructor.declarationDescriptor
        return if (descriptor is TypeParameterDescriptor) {
            val upperBound = descriptor.upperBounds.singleOrNull() ?:
                TODO("$descriptor : ${descriptor.upperBounds}")

            upperBound.erasure()
        } else {
            this
        }
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()
        return if (expression.argument.type.isSubtypeOf(typeOperand)) {
            // TODO: consider the case when expression type is wrong e.g. due to generics-related unchecked casts.
            expression.argument
        } else if (typeOperand == expression.typeOperand) {
            expression
        } else {
            builder.at(expression).irAs(expression.argument, typeOperand)
        }
    }

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()

        return builder.irBlock(expression) {
            +irLet(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable), typeOperand),
                        thenPart = irImplicitCast(irGet(variable), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}