/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.AbstractValueUsageTransformer
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render


// Copied and adapted from Kotlin/Native

abstract class AbstractValueUsageLowering(val context: JsCommonBackendContext) : AbstractValueUsageTransformer(context.irBuiltIns),
    BodyLoweringPass {

    val icUtils = context.inlineClassesUtils

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO workaround for callable references
        // Prevents from revisiting local
        if (container.parent is IrFunction) return

        val replacement = container.transform(this, null) as IrDeclaration

        if (container !== replacement) error("Declaration has changed: ${container}")

        // TODO: Track & insert parents for temporary variables
        irBody.patchDeclarationParents(container as? IrDeclarationParent ?: container.parent)
    }


    abstract fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression

    override fun IrExpression.useAs(type: IrType): IrExpression {
        val actualType = when (this) {
            is IrConstructorCall -> symbol.owner.returnType
            is IrCall -> symbol.owner.realOverrideTarget.returnType
            is IrGetField -> this.symbol.owner.type

            is IrTypeOperatorCall -> {
                if (operator == IrTypeOperator.REINTERPRET_CAST) {
                    this.typeOperand
                } else {
                    this.type
                }
            }

            is IrGetValue -> {
                val value = this.symbol.owner
                if (value is IrValueParameter && icUtils.shouldValueParameterBeBoxed(value)) {
                    irBuiltIns.anyType
                } else {
                    this.type
                }
            }

            else -> this.type
        }

        return useExpressionAsType(actualType, type)
    }


    private val IrFunctionAccessExpression.target: IrFunction
        get() = when (this) {
            is IrConstructorCall -> this.symbol.owner
            is IrDelegatingConstructorCall -> this.symbol.owner
            is IrCall -> this.callTarget
            else -> TODO(this.render())
        }

    private val IrCall.callTarget: IrFunction
        get() = symbol.owner.realOverrideTarget


    override fun IrExpression.useAsDispatchReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return if (expression.symbol.owner.dispatchReceiverParameter?.let { icUtils.shouldValueParameterBeBoxed(it) } == true)
            this.useAs(irBuiltIns.anyType)
        else
            this.useAsArgument(expression.target.dispatchReceiverParameter!!)
    }

    override fun IrExpression.useAsExtensionReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.extensionReceiverParameter!!)
    }

    override fun IrExpression.useAsValueArgument(
        expression: IrFunctionAccessExpression,
        parameter: IrValueParameter
    ): IrExpression {

        return this.useAsArgument(expression.target.valueParameters[parameter.index])
    }


    override fun IrExpression.useAsVarargElement(expression: IrVararg): IrExpression {
        return this.useAs(
            // Do not box primitive inline classes
            if (icUtils.isTypeInlined(type) && !icUtils.isTypeInlined(expression.type) && !expression.type.isPrimitiveArray())
                irBuiltIns.anyNType
            else
                expression.varargElementType
        )
    }
}

class AutoboxingTransformer(context: JsCommonBackendContext) : AbstractValueUsageLowering(context) {
    override fun IrExpression.useExpressionAsType(actualType: IrType, expectedType: IrType): IrExpression {
        // // TODO: Default parameters are passed as nulls and they need not to be unboxed. Fix this

        if (actualType.makeNotNull().isNothing())
            return this

        if (actualType.isUnit() && !expectedType.isUnit()) {
            // Don't materialize Unit if value is known to be proper Unit on runtime
            if (!this.isGetUnit()) {
                val unitValue = JsIrBuilder.buildGetObjectValue(actualType, context.irBuiltIns.unitClass)
                return JsIrBuilder.buildComposite(actualType, listOf(this, unitValue))
            }
        }

        val actualInlinedClass = icUtils.getInlinedClass(actualType)
        val expectedInlinedClass = icUtils.getInlinedClass(expectedType)

        // Mimicking behaviour of current JS backend
        // TODO: Revisit
        if (
            (actualType is IrDynamicType && expectedType.makeNotNull().isChar()) ||
            (actualType.makeNotNull().isChar() && expectedType is IrDynamicType)
        ) return this

        val function = when {
            actualInlinedClass == null && expectedInlinedClass == null -> return this
            actualInlinedClass != null && expectedInlinedClass == null -> icUtils.boxIntrinsic
            actualInlinedClass == null && expectedInlinedClass != null -> icUtils.unboxIntrinsic
            else -> return this
        }

        return buildSafeCall(this, actualType, expectedType) { arg ->
            JsIrBuilder.buildCall(
                function,
                expectedType,
                typeArguments = listOf(actualType, expectedType)
            ).also {
                it.putValueArgument(0, arg)
            }
        }
    }

    private tailrec fun IrExpression.isGetUnit(): Boolean =
        when (this) {
            is IrContainerExpression ->
                when (val lastStmt = this.statements.lastOrNull()) {
                    is IrExpression -> lastStmt.isGetUnit()
                    else -> false
                }

            is IrGetObjectValue ->
                this.symbol == irBuiltIns.unitClass

            else -> false
        }

    private fun buildSafeCall(
        arg: IrExpression,
        actualType: IrType,
        resultType: IrType,
        call: (IrExpression) -> IrExpression
    ): IrExpression {
        // Safe call is only needed if we cast from Nullable type to Nullable type.
        // Otherwise, null value cannot occur.
        if (!actualType.isNullable() || !resultType.isNullable())
            return call(arg)

        return JsIrBuilder.run {
            // TODO: Set parent of local variables
            val tmp = buildVar(actualType, parent = null, initializer = arg)
            val nullCheck = buildIfElse(
                type = resultType,
                cond = buildCall(irBuiltIns.eqeqSymbol).apply {
                    putValueArgument(0, buildGetValue(tmp.symbol))
                    putValueArgument(1, buildNull(irBuiltIns.nothingNType))
                },
                thenBranch = buildNull(irBuiltIns.nothingNType),
                elseBranch = call(buildGetValue(tmp.symbol))
            )
            buildBlock(
                type = resultType,
                statements = listOf(
                    tmp,
                    nullCheck
                )
            )
        }
    }
}