/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.SpecialBridgeMethods
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.hasPlatformDependent
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

val jvmArgumentNullabilityAssertions = makeIrFilePhase(
    ::JvmArgumentNullabilityAssertionsLowering,
    name = "ArgumentNullabilityAssertions",
    description = "Transform nullability assertions on arguments according to the compiler settings",
    // jvmStringConcatenationLowering may remove IMPLICIT_NOTNULL casts.
    prerequisite = setOf(jvmStringConcatenationLowering)
)

private enum class AssertionScope {
    Enabled, Disabled
}

private class JvmArgumentNullabilityAssertionsLowering(context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformer<AssertionScope> {

    private val isWithUnifiedNullChecks = context.config.unifiedNullChecks
    private val isCallAssertionsDisabled = context.config.isCallAssertionsDisabled
    private val isReceiverAssertionsDisabled = context.config.isReceiverAssertionsDisabled

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    override fun lower(irFile: IrFile) = irFile.transformChildren(this, AssertionScope.Enabled)

    override fun visitElement(element: IrElement, data: AssertionScope): IrElement =
        super.visitElement(element, AssertionScope.Enabled)

    override fun visitDeclaration(declaration: IrDeclarationBase, data: AssertionScope): IrStatement =
        super.visitDeclaration(declaration, AssertionScope.Enabled)

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: AssertionScope): IrExpression =
        if (expression.operator == IrTypeOperator.IMPLICIT_NOTNULL && (data == AssertionScope.Disabled || isCallAssertionsDisabled))
            expression.argument.transform(this, data)
        else
            super.visitTypeOperator(expression, data)

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: AssertionScope): IrElement {
        // Always drop nullability assertions on dispatch receivers, assuming that it will throw NPE.
        //
        // NB there are some members in Kotlin built-in classes which are NOT implemented as platform method calls,
        // and thus break this assertion - e.g., 'Array<T>.iterator()' and similar functions.
        // See KT-30908 for more details.
        expression.dispatchReceiver = expression.dispatchReceiver?.transform(this, AssertionScope.Disabled)

        val receiverAssertionScope = if (
            isReceiverAssertionsDisabled ||
            !isWithUnifiedNullChecks && expression.origin.isOperatorWithNoNullabilityAssertionsOnExtensionReceiver
        ) AssertionScope.Disabled else AssertionScope.Enabled

        expression.extensionReceiver = expression.extensionReceiver?.transform(this, receiverAssertionScope)

        val parameterAssertionScope =
            if (isCallToMethodWithTypeCheckBarrier(expression)) AssertionScope.Disabled else AssertionScope.Enabled
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)?.let { irArgument ->
                expression.putValueArgument(i, irArgument.transform(this, parameterAssertionScope))
            }
        }

        return expression
    }

    override fun visitGetField(expression: IrGetField, data: AssertionScope): IrExpression {
        expression.receiver = expression.receiver?.transform(this, AssertionScope.Disabled)
        return expression
    }

    override fun visitSetField(expression: IrSetField, data: AssertionScope): IrExpression {
        expression.receiver = expression.receiver?.transform(this, AssertionScope.Disabled)
        expression.value = expression.value.transform(this, data)
        return expression
    }

    private fun isCallToMethodWithTypeCheckBarrier(expression: IrMemberAccessExpression<*>): Boolean =
        (expression.symbol.owner as? IrSimpleFunction)
            ?.let {
                val bridgeInfo = specialBridgeMethods.findSpecialWithOverride(it, includeSelf = true)
                // The JVM BE adds null checks around platform dependent special bridge methods (Map.getOrDefault and the version of
                // MutableMap.remove with two arguments).
                bridgeInfo != null && !bridgeInfo.first.hasPlatformDependent()
            } == true

    private val IrStatementOrigin?.isOperatorWithNoNullabilityAssertionsOnExtensionReceiver
        get() = this is IrStatementOrigin.COMPONENT_N || this in operatorsWithNoNullabilityAssertionsOnExtensionReceiver

    companion object {
        private val operatorsWithNoNullabilityAssertionsOnExtensionReceiver = hashSetOf(
            IrStatementOrigin.PREFIX_INCR, IrStatementOrigin.POSTFIX_INCR,
            IrStatementOrigin.PREFIX_DECR, IrStatementOrigin.POSTFIX_DECR
        )
    }
}
