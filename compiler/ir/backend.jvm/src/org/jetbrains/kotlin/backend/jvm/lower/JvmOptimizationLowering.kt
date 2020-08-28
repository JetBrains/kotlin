/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.intrinsics.Not
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal val jvmOptimizationLoweringPhase = makeIrFilePhase(
    ::JvmOptimizationLowering,
    name = "JvmOptimizationLowering",
    description = "Optimize code for JVM code generation"
)

class JvmOptimizationLowering(val context: JvmBackendContext) : FileLoweringPass {

    companion object {
        fun isNegation(expression: IrExpression, context: JvmBackendContext): Boolean =
            expression is IrCall &&
                    context.state.intrinsics.getIntrinsic(expression.symbol.descriptor) is Not
    }

    private val IrFunction.isObjectEquals
        get() = name.asString() == "equals" &&
                valueParameters.count() == 1 &&
                valueParameters[0].type.isNullableAny() &&
                extensionReceiverParameter == null &&
                dispatchReceiverParameter != null


    private fun getOperandsIfCallToEQEQOrEquals(call: IrCall): Pair<IrExpression, IrExpression>? =
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
        val transformer = object : IrElementTransformer<IrClass?> {

            // Thread the current class through the transformations in order to replace
            // final default accessor calls with direct backing field access when
            // possible.
            override fun visitClass(declaration: IrClass, data: IrClass?): IrStatement {
                declaration.transformChildren(this, declaration)
                return declaration
            }

            // For some functions, we clear the current class field since the code could end up
            // in another class then the one it is nested under in the IR.
            // TODO: Loosen this up for local functions for lambdas passed as an inline lambda
            // argument to an inline function. In that case the code does end up in the current class.
            override fun visitFunction(declaration: IrFunction, data: IrClass?): IrStatement {
                val codeMightBeGeneratedInDifferentClass = declaration.isSuspend ||
                        declaration.isInline ||
                        declaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                declaration.transformChildren(this, data.takeUnless { codeMightBeGeneratedInDifferentClass })
                return declaration
            }

            override fun visitCall(expression: IrCall, data: IrClass?): IrExpression {
                expression.transformChildren(this, data)

                removeIntTypeSafeCastsForEquality(expression)

                if (expression.symbol.owner.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
                    if (data == null) return expression
                    val simpleFunction = (expression.symbol.owner as? IrSimpleFunction) ?: return expression
                    val property = simpleFunction.correspondingPropertySymbol?.owner ?: return expression
                    if (property.isLateinit) return expression
                    return optimizePropertyAccess(expression, simpleFunction, property, data)
                }

                if (isNegation(expression, context) && isNegation(expression.dispatchReceiver!!, context)) {
                    return (expression.dispatchReceiver as IrCall).dispatchReceiver!!
                }

                getOperandsIfCallToEQEQOrEquals(expression)?.let { (left, right) ->
                    return when {
                        left.isNullConst() && right.isNullConst() ->
                            IrConstImpl.constTrue(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                        left.isNullConst() && right is IrConst<*> || right.isNullConst() && left is IrConst<*> ->
                            IrConstImpl.constFalse(expression.startOffset, expression.endOffset, context.irBuiltIns.booleanType)

                        else -> expression
                    }
                }

                return expression
            }

            private fun IrType.isByteOrShort() = isByte() || isShort()

            // For `==` and `!=`, get rid of safe calls to convert `Byte?` or `Short?` to `Int?`.
            // For equality, we do not need to perform such conversions as the builtin for equality
            // will handle it. Having the safe call leads to unnecessary null checks and boxing.
            private fun removeIntTypeSafeCastsForEquality(expression: IrCall) {
                if (expression.origin == IrStatementOrigin.EQEQ || expression.origin == IrStatementOrigin.EXCLEQ) {
                    for (i in 0 until expression.valueArgumentsCount) {
                        if (expression.getValueArgument(i)!!.type.makeNotNull().isInt()) {
                            val argument = expression.getValueArgument(i)!!
                            if (argument is IrBlock && argument.origin == IrStatementOrigin.SAFE_CALL) {
                                if (argument.statements.size == 2) {
                                    val variable = argument.statements[0]
                                    if (variable is IrVariable && variable.type.makeNotNull().isByteOrShort()) {
                                        val whenExpression = argument.statements[1]
                                        if (whenExpression is IrWhen && whenExpression.branches.size == 2) {
                                            val secondBranch = whenExpression.branches[1]
                                            if (secondBranch is IrElseBranch && secondBranch.result is IrCall) {
                                                val conversion = secondBranch.result as IrCall
                                                if (conversion.symbol.owner.name.asString() == "toInt" &&
                                                    conversion.dispatchReceiver is IrGetValue &&
                                                    (conversion.dispatchReceiver as IrGetValue).symbol.owner == variable
                                                ) {
                                                    expression.putValueArgument(i, variable.initializer)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            private fun optimizePropertyAccess(
                expression: IrCall,
                accessor: IrSimpleFunction,
                property: IrProperty,
                currentClass: IrClass
            ): IrExpression {
                if (accessor.parentAsClass == currentClass &&
                    property.backingField?.parentAsClass == currentClass &&
                    accessor.modality == Modality.FINAL &&
                    !accessor.isExternal
                ) {
                    val backingField = property.backingField!!
                    val receiver = expression.dispatchReceiver
                    return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                        if (backingField.isStatic && receiver != null) {
                            // If the field is static, evaluate the receiver for potential side effects.
                            +receiver.coerceToUnit(context.irBuiltIns)
                        }
                        if (accessor.valueParameters.size > 0) {
                            +irSetField(
                                receiver.takeUnless { backingField.isStatic },
                                backingField,
                                expression.getValueArgument(expression.valueArgumentsCount - 1)!!
                            )
                        } else {
                            +irGetField(receiver.takeUnless { backingField.isStatic }, backingField)
                        }
                    }
                }
                return expression
            }

            override fun visitWhen(expression: IrWhen, data: IrClass?): IrExpression {
                val isCompilerGenerated = expression.origin == null
                expression.transformChildren(this, data)
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

            override fun visitBlockBody(body: IrBlockBody, data: IrClass?): IrBody {
                body.transformChildren(this, data)
                removeUnnecessaryTemporaryVariables(body.statements)
                return body
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: IrClass?): IrExpression {
                expression.transformChildren(this, data)
                removeUnnecessaryTemporaryVariables(expression.statements)
                return expression
            }

            override fun visitGetValue(expression: IrGetValue, data: IrClass?): IrExpression {
                // Replace IrGetValue of an immutable temporary variable with a constant
                // initializer with the constant initializer.
                val variable = expression.symbol.owner
                return if (isImmutableTemporaryVariableWithConstantValue(variable))
                    ((variable as IrVariable).initializer!! as IrConst<*>).copy()
                else
                    expression
            }
        }
        irFile.transformChildren(transformer, null)
    }
}
