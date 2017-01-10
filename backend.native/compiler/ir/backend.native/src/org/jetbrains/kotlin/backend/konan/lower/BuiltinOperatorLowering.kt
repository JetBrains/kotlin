package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

/**
 * This lowering pass lowers some calls to [IrBuiltinOperatorDescriptor]s.
 */
internal class BuiltinOperatorLowering(val context: Context) : BodyLoweringPass {

    private val transformer = BuiltinOperatorTransformer(context)

    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(transformer)
    }

}

private class BuiltinOperatorTransformer(val context: Context) : IrElementTransformerVoid() {

    private val builtIns = context.builtIns
    private val irBuiltins = context.irModule!!.irBuiltins

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.descriptor is IrBuiltinOperatorDescriptor) {
            return transformBuiltinOperator(expression)
        }

        return expression
    }

    private fun transformBuiltinOperator(expression: IrCall): IrExpression {
        val descriptor = expression.descriptor

        return when (descriptor) {
            irBuiltins.eqeq -> {
                val binary = expression as IrBinaryPrimitiveImpl
                lowerEqeq(binary.argument0, binary.argument1, expression.startOffset, expression.endOffset)
            }

            irBuiltins.throwNpe -> IrCallImpl(expression.startOffset, expression.endOffset,
                    builtIns.getKonanInternalFunctions("ThrowNullPointerException").single())

            irBuiltins.noWhenBranchMatchedException -> IrCallImpl(expression.startOffset, expression.endOffset,
                    builtIns.getKonanInternalFunctions("ThrowNoWhenBranchMatchedException").single())

            else -> expression
        }
    }

    private fun lowerEqeq(lhs: IrExpression, rhs: IrExpression, startOffset: Int, endOffset: Int): IrExpression {
        // TODO: optimize boxing?

        // TODO: areEqualByValue intrinsics are specially treated by code generator
        // and thus can be declared synthetically in the compiler instead of explicitly in the runtime.

        // Find a type-compatible `konan.internal.areEqualByValue` intrinsic:
        val equals = builtIns.getKonanInternalFunctions("areEqualByValue").firstOrNull {
            lhs.type.isSubtypeOf(it.valueParameters[0].type) && rhs.type.isSubtypeOf(it.valueParameters[1].type)
        } ?: builtIns.getKonanInternalFunctions("areEqual").single() // or use the general implementation.

        return IrCallImpl(startOffset, endOffset, equals).apply {
            putValueArgument(0, lhs)
            putValueArgument(1, rhs)
        }
    }
}
