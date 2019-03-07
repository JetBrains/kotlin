/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE

class TrimMargin : IntrinsicMethod() {
    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        return tryApply(resolvedCall, codegen)
            ?: codegen.state.typeMapper.mapToCallableMethod(fd, false)
    }

    private fun tryApply(resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable? {
        val literalText = resolvedCall.getReceiverExpression()
            ?.let { codegen.getCompileTimeConstant(it) as? StringValue }
            ?.value ?: return null

        val text = when (val argument = resolvedCall.valueArguments.values.single()) {
            is DefaultValueArgument -> literalText.trimMargin()
            is ExpressionValueArgument -> {
                val marginPrefix = argument.valueArgument?.getArgumentExpression()
                    ?.let { codegen.getCompileTimeConstant(it) as? StringValue }
                    ?.value ?: return null
                literalText.trimMargin(marginPrefix)
            }
            else -> error("Unknown value argument type ${argument::class}: $argument")
        }
        return StringConstant(text)
    }
}

class TrimIndent : IntrinsicMethod() {
    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        return tryApply(resolvedCall, codegen)
            ?: codegen.state.typeMapper.mapToCallableMethod(fd, false)
    }

    private fun tryApply(resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable? {
        val literalText = resolvedCall.getReceiverExpression()
            ?.let { codegen.getCompileTimeConstant(it) as? StringValue }
            ?.value ?: return null

        val text = literalText.trimIndent()
        return StringConstant(text)
    }
}

private class StringConstant(private val text: String) : IntrinsicCallable(JAVA_STRING_TYPE, emptyList(), null, null), IntrinsicWithSpecialReceiver {
    override fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, codegen: ExpressionCodegen) =
        StackValue.constant(text, JAVA_STRING_TYPE)
}
