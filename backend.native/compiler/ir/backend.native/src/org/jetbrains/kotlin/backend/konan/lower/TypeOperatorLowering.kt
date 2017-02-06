package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable

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

    private val builder = context.createIrBuilder(function)

    val throwClassCastException by lazy {
        context.builtIns.getKonanInternalFunctions("ThrowClassCastException").single()
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        // ignore inner functions during this pass
        return declaration
    }

    private fun KotlinType.erasure(): KotlinType {
        val descriptor = this.constructor.declarationDescriptor
        return when (descriptor) {
            is ClassDescriptor -> this
            is TypeParameterDescriptor -> {
                val upperBound = descriptor.upperBounds.singleOrNull() ?:
                        TODO("$descriptor : ${descriptor.upperBounds}")

                if (this.isMarkedNullable) {
                    // `T?`
                    upperBound.erasure().makeNullable()
                } else {
                    upperBound.erasure()
                }
            }
            else -> TODO(this.toString())
        }
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = expression.typeOperand.erasure()

        assert (!TypeUtils.hasNullableSuperType(typeOperand)) // So that `isNullable()` <=> `isMarkedNullable`.

        // TODO: consider the case when expression type is wrong e.g. due to generics-related unchecked casts.

        return when {
            expression.argument.type.isSubtypeOf(typeOperand) -> expression.argument

            expression.argument.type.isNullable() -> {
                with (builder) {
                    irLet(expression.argument) { argument ->
                        irIfThenElse(
                                type = expression.type,
                                condition = irEqeqeq(irGet(argument), irNull()),

                                thenPart = if (typeOperand.isMarkedNullable)
                                    irNull()
                                else
                                    irCall(throwClassCastException),

                                elsePart = irAs(irGet(argument), typeOperand.makeNotNullable())
                        )
                    }
                }
            }

            typeOperand.isMarkedNullable -> builder.irAs(expression.argument, typeOperand.makeNotNullable())

            typeOperand == expression.typeOperand -> expression

            else -> builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun KotlinType.isNullable() = TypeUtils.isNullableType(this)

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