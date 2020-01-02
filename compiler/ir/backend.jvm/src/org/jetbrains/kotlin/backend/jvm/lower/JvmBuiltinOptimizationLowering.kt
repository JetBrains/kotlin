/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.codegen.intrinsics.Not
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val jvmBuiltinOptimizationLoweringPhase = makeIrFilePhase(
    ::JvmBuiltinOptimizationLowering,
    name = "JvmBuiltinOptimizationLowering",
    description = "Optimize builtin calls for JVM code generation"
)

class JvmBuiltinOptimizationLowering(val context: JvmBackendContext) : FileLoweringPass {

    companion object {
        fun isNegation(expression: IrExpression, context: JvmBackendContext): Boolean =
            expression is IrCall &&
                    context.state.intrinsics.getIntrinsic(expression.symbol.descriptor) is Not
    }

    private fun hasNoSideEffectsForNullCompare(expression: IrExpression): Boolean {
        return expression.type.isPrimitiveType() && (expression is IrConst<*> || expression is IrGetValue)
    }

    private val IrFunction.isObjectEquals
        get() = name.asString() == "equals" &&
                valueParameters.count() == 1 &&
                valueParameters[0].type.isNullableAny() &&
                extensionReceiverParameter == null &&
                dispatchReceiverParameter != null


    private fun getOperandsIfCallToEqeqOrEquals(call: IrCall): Pair<IrExpression, IrExpression>? =
        when {
            call.symbol == context.irBuiltIns.eqeqSymbol -> {
                val left = call.getValueArgument(0)!!
                val right = call.getValueArgument(1)!!
                left to right
            }

            call.symbol.owner.isObjectEquals -> {
                val left = call.dispatchReceiver!!
                val right = call.getValueArgument(0)!!
                left to right
            }

            else -> null
        }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (isNegation(expression, context) && isNegation(expression.dispatchReceiver!!, context)) {
                    return (expression.dispatchReceiver as IrCall).dispatchReceiver!!
                }

                getOperandsIfCallToEqeqOrEquals(expression)?.let { (left, right) ->
                    val constFalse =
                        IrConstImpl.constFalse(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                    return when {
                        left.isNullConst() && right.isNullConst() ->
                            IrConstImpl.constTrue(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                        left.isNullConst() && right is IrConst<*> || right.isNullConst() && left is IrConst<*> ->
                            constFalse

                        right.isNullConst() && left.type.unboxInlineClass().isPrimitiveType() ||
                                left.isNullConst() && right.type.unboxInlineClass().isPrimitiveType() -> {
                            val nonNullArgument = if (left.isNullConst()) right else left
                            if (hasNoSideEffectsForNullCompare(nonNullArgument)) {
                                constFalse
                            } else {
                                IrBlockImpl(expression.startOffset, expression.endOffset, expression.type, expression.origin).apply {
                                    statements.add(
                                        nonNullArgument.coerceToUnitIfNeeded(
                                            nonNullArgument.type.toKotlinType(),
                                            context.irBuiltIns
                                        )
                                    )
                                    statements.add(constFalse)
                                }
                            }
                        }

                        else -> expression
                    }
                }

                return expression
            }

            override fun visitWhen(expression: IrWhen): IrExpression {
                val isCompilerGenerated = expression.origin == null
                expression.transformChildrenVoid(this)
                // Remove all branches with constant false condition.
                expression.branches.removeIf {
                    it.condition.isFalseConst() && isCompilerGenerated
                }
                if (expression.origin == IrStatementOrigin.ANDAND) {
                    assert(
                        expression.type.isBoolean()
                                && expression.branches.size == 2
                                && expression.branches[1].condition.isTrueConst()
                                && expression.branches[1].result.isFalseConst()
                    ) {
                        "ANDAND condition should have an 'if true then false' body on its second branch. " +
                                "Failing expression: ${expression.dump()}"
                    }
                    // Replace conjunction condition with intrinsic "and" function call
                    return IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        context.irBuiltIns.booleanType,
                        context.irBuiltIns.andandSymbol
                    ).apply {
                        putValueArgument(0, expression.branches[0].condition)
                        putValueArgument(1, expression.branches[0].result)
                    }
                }
                if (expression.origin == IrStatementOrigin.OROR) {
                    assert(
                        expression.type.isBoolean()
                                && expression.branches.size == 2
                                && expression.branches[0].result.isTrueConst()
                                && expression.branches[1].condition.isTrueConst()
                    ) {
                        "OROR condition should have an 'if a then true' body on its first branch, " +
                                "and an 'if true then b' body on its second branch. " +
                                "Failing expression: ${expression.dump()}"
                    }
                    return IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        context.irBuiltIns.booleanType,
                        context.irBuiltIns.ororSymbol
                    ).apply {
                        putValueArgument(0, expression.branches[0].condition)
                        putValueArgument(1, expression.branches[1].result)
                    }
                }
                // If the only condition that is left has a constant true condition remove the
                // when in favor of the result. If there are no conditions left, remove the when
                // entirely and replace it with an empty block.
                return if (expression.branches.size == 0) {
                    IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)
                } else {
                    expression.branches.first().takeIf { it.condition.isTrueConst() && isCompilerGenerated }?.result ?: expression
                }
            }

            private fun isImmutableTemporaryVariableWithConstantValue(statement: IrStatement): Boolean {
                return statement is IrVariable &&
                        statement.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE &&
                        !statement.isVar &&
                        statement.initializer is IrConst<*>
            }

            private fun removeUnnecessaryTemporaryVariables(statements: MutableList<IrStatement>) {
                // Remove declarations of immutable temporary variables with constant values.
                // IrGetValue operations for such temporary variables are replaced
                // by the initializer IrConst. This makes sure that we do not load and
                // store constants in/from locals. For example
                //
                //     "StringConstant"!!
                //
                // introduces a temporary variable for the string constant and generates
                // a null check
                //
                //     block
                //       temp = "StringConstant"
                //       when (eq(temp, null))
                //          (true) -> throwNpe()
                //          (false) -> temp
                //
                // When generating code, this stores the string constant in a local and loads
                // it from there. The removal of the temporary and the replacement of the loads
                // of the temporary (see visitGetValue) with the constant avoid generating local
                // loads and stores by turning this into
                //
                //     block
                //       when (eq("StringConstant", null))
                //          (true) -> throwNpe()
                //          (false) -> "StringConstant"
                //
                // which allows the equality check to be simplified away and we end up with
                // just a const string load.
                statements.removeIf {
                    isImmutableTemporaryVariableWithConstantValue(it)
                }

                // Remove a block that contains only two statements: the declaration of a temporary
                // variable and a load of the value of that temporary variable with just the initializer
                // for the temporary variable. We only perform this transformation for compiler generated
                // temporary variables. Local variables can be changed at runtime and therefore eliminating
                // an actual local variable changes debugging behavior.
                //
                // This helps avoid temporary variables even for side-effecting expressions when they are
                // not needed. Having a temporary variable leads to local loads and stores in the
                // generated java bytecode which are not necessary. For example
                //
                //     42.toLong()!!
                //
                // introduces a temporary variable for the toLong() call and a null check
                //    block
                //      temp = 42.toLong()
                //      when (eq(temp, null))
                //        (true) -> throwNep()
                //        (false) -> temp
                //
                // the when is simplified because long is a primitive type, which leaves us with
                //
                //    block
                //      temp = 42.toLong()
                //      temp
                //
                // which can be simplified to simply
                //
                //    block
                //      42.toLong()
                //
                // Doing so we avoid local loads and stores.
                if (statements.size == 2) {
                    val first = statements[0]
                    val second = statements[1]
                    if (first is IrVariable
                        && first.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                        && second is IrGetValue
                        && first.symbol == second.symbol
                    ) {
                        statements.clear()
                        first.initializer?.let { statements.add(it) }
                    }
                }
            }

            override fun visitBlockBody(body: IrBlockBody): IrBody {
                body.transformChildrenVoid(this)
                removeUnnecessaryTemporaryVariables(body.statements)
                return body
            }

            override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
                expression.transformChildrenVoid(this)
                removeUnnecessaryTemporaryVariables(expression.statements)
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                // Replace IrGetValue of an immutable temporary variable with a constant
                // initializer with the constant initializer.
                val variable = expression.symbol.owner
                return if (isImmutableTemporaryVariableWithConstantValue(variable))
                    ((variable as IrVariable).initializer!! as IrConst<*>).copy()
                else
                    expression
            }
        })
    }
}
