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
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.Companion.ENABLE_ASSERTIONS
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.backend.konan.util.atMostOne
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.util.type
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit

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

    private val assertFqName = "kotlin.assert"

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val descriptor = expression.descriptor

        // TODO: use stdlib assert's descriptors instead of fqName
        // Replace assert() call with an empty composite if assertions are not enabled.
        if (descriptor.fqNameSafe.asString() == assertFqName &&
                !context.config.configuration.getBoolean(ENABLE_ASSERTIONS)) {
            assert(expression.type.isUnit())
            return IrCompositeImpl(expression.startOffset, expression.endOffset, expression.type)
        }

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

    private fun transformBuiltinOperator(expression: IrCall): IrExpression {
        val descriptor = expression.descriptor

        return when (descriptor) {
            irBuiltins.eqeq -> {
                lowerEqeq(expression.getValueArgument(0)!!, expression.getValueArgument(1)!!,
                        expression.startOffset, expression.endOffset)
            }

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
            lowerEqeq(lhs, rhs, expression.startOffset, expression.endOffset)
            // Note: such comparisons are deprecated.
        } else {
            expression
        }
    }

    private fun lowerEqeq(lhs: IrExpression, rhs: IrExpression, startOffset: Int, endOffset: Int): IrExpression {
        // TODO: optimize boxing?

        val equals = selectEqualsFunction(lhs, rhs)

        return IrCallImpl(startOffset, endOffset, equals).apply {
            putValueArgument(0, lhs)
            putValueArgument(1, rhs)
        }
    }

    private fun selectEqualsFunction(lhs: IrExpression, rhs: IrExpression): IrSimpleFunctionSymbol {
        val nullableNothingType = builtIns.nullableNothingType
        if (lhs.type.isSubtypeOf(nullableNothingType) && rhs.type.isSubtypeOf(nullableNothingType)) {
            // Compare by reference if each part is either `Nothing` or `Nothing?`:
            return irBuiltins.eqeqeqSymbol
        }

        // TODO: areEqualByValue intrinsics are specially treated by code generator
        // and thus can be declared synthetically in the compiler instead of explicitly in the runtime.

        // Find a type-compatible `konan.internal.areEqualByValue` intrinsic:
        context.ir.symbols.areEqualByValue.atMostOne {
            lhs.type.isSubtypeOf(it.owner.valueParameters[0].type) &&
                    rhs.type.isSubtypeOf(it.owner.valueParameters[1].type)
        }?.let {
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
}
