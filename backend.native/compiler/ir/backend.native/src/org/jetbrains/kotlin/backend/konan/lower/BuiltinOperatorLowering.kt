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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
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
        val descriptor = expression.descriptor

        if (descriptor is IrBuiltinOperatorDescriptor) {
            return transformBuiltinOperator(expression)
        }

        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.argument.type.isNothing()) {
            return expression.argument
        }
        return expression
    }

    private fun isIeee754Equals(descriptor: FunctionDescriptor): Boolean =
            irBuiltins.ieee754equalsFunByOperandType.values.any { it.descriptor == descriptor }

    private fun transformBuiltinOperator(expression: IrCall): IrExpression {
        val descriptor = expression.descriptor

        // IEEE754 comparison for floating point values are done by intrinsic
        if (isIeee754Equals(descriptor)) return lowerEqeq(expression)

        return when (descriptor) {
            irBuiltins.eqeq -> lowerEqeq(expression)

            irBuiltins.eqeqeq -> lowerEqeqeq(expression)

            irBuiltins.throwNpe -> IrCallImpl(expression.startOffset, expression.endOffset,
                    context.ir.symbols.ThrowNullPointerException)

            irBuiltins.noWhenBranchMatchedException -> IrCallImpl(expression.startOffset, expression.endOffset,
                    context.ir.symbols.ThrowNoWhenBranchMatchedException)

            else -> expression
        }
    }

    private fun lowerEqeqeq(expression: IrCall): IrExpression {
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!

        return if (lhs.type.isValueType() && rhs.type.isValueType()) {
            // Achieve the same behavior as with JVM BE: if both sides of `===` are values, then compare by value:
            lowerEqeq(expression)
            // Note: such comparisons are deprecated.
        } else {
            expression
        }
    }

    private fun lowerEqeq(expression: IrCall): IrExpression {
        // TODO: optimize boxing?
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset

        val equals = selectEqualsFunction(expression)

        return IrCallImpl(startOffset, endOffset, equals).apply {
            putValueArgument(0, expression.getValueArgument(0)!!)
            putValueArgument(1, expression.getValueArgument(1)!!)
        }
    }

    private fun selectEqualsFunction(expression: IrCall): IrSimpleFunctionSymbol {
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!

        val nullableNothingType = builtIns.nullableNothingType
        if (lhs.type.isSubtypeOf(nullableNothingType) && rhs.type.isSubtypeOf(nullableNothingType)) {
            // Compare by reference if each part is either `Nothing` or `Nothing?`:
            return irBuiltins.eqeqeqSymbol
        }

        // TODO: areEqualByValue and ieee754Equals intrinsics are specially treated by code generator
        // and thus can be declared synthetically in the compiler instead of explicitly in the runtime.

        // Find a type-compatible `konan.internal.ieee754Equals` intrinsic:
        if (isIeee754Equals(expression.descriptor)) {
            // FIXME: intrinsic should be also compatible with nullable types
            selectIntrinsic(context.ir.symbols.ieee754Equals, lhs.type, rhs.type)?.let {
                return it
            }
        }

        // Find a type-compatible `konan.internal.areEqualByValue` intrinsic:
        selectIntrinsic(context.ir.symbols.areEqualByValue, lhs.type, rhs.type)?.let {
            return it
        }

        return if (lhs.isNullConst() || rhs.isNullConst()) {
            // or compare by reference if left or right part is `null`:
            irBuiltins.eqeqeqSymbol
        } else {
            // or use the general implementation:
            context.ir.symbols.areEqual
        }
    }

    private fun selectIntrinsic(from: List<IrSimpleFunctionSymbol>, lhsType: KotlinType, rhsType: KotlinType):
            IrSimpleFunctionSymbol? {
        return from.atMostOne {
            lhsType.isSubtypeOf(it.owner.valueParameters[0].type) && rhsType.isSubtypeOf(it.owner.valueParameters[1].type)
        }
    }
}
