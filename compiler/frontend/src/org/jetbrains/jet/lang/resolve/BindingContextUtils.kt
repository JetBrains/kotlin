/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.bindingContextUtil

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.Call
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContext.CALL
import org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext.LABEL_TARGET
import org.jetbrains.jet.lang.resolve.BindingContext.FUNCTION
import org.jetbrains.jet.lang.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.jet.lang.resolve.calls.ArgumentTypeResolver
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.psi.JetUnaryExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import org.jetbrains.jet.lang.psi.psiUtil.getCalleeExpressionIfAny
import org.jetbrains.jet.lang.resolve.calls.CallTransformer.CallForImplicitInvoke
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.psi.JetCallElement
import org.jetbrains.jet.lang.psi.psiUtil.getTextWithLocation

/**
 *  For expressions like <code>a(), a[i], a.b.c(), +a, a + b, (a()), a(): Int, @label a()</code>
 *  returns a corresponding call.
 *
 *  Note: special construction like <code>a!!, a ?: b, if (c) a else b</code> are resolved as calls,
 *  so there is a corresponding call for them.
 */
public fun JetElement.getCall(context: BindingContext): Call? {
    val element = if (this is JetExpression) JetPsiUtil.deparenthesize(this) else this
    if (element == null) return null

    val parent = element.getParent()
    val reference = when {
        parent is JetThisExpression -> parent : JetThisExpression
        else -> element.getCalleeExpressionIfAny()
    }
    if (reference != null) {
        return context[CALL, reference]
    }
    return context[CALL, element]
}

public fun JetElement.getParentCall(context: BindingContext, strict: Boolean = true): Call? {
    val callExpressionTypes = array<Class<out JetElement>?>(
            javaClass<JetSimpleNameExpression>(), javaClass<JetCallElement>(), javaClass<JetBinaryExpression>(),
            javaClass<JetUnaryExpression>(), javaClass<JetArrayAccessExpression>())

    val parent = if (strict) {
        PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
    } else {
        PsiTreeUtil.getNonStrictParentOfType(this, *callExpressionTypes)
    }
    return parent?.getCall(context)
}

public fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return context[RESOLVED_CALL, this]
}

public fun JetElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return this?.getCall(context)?.getResolvedCall(context)
}

public fun JetElement?.getParentResolvedCall(context: BindingContext, strict: Boolean = true): ResolvedCall<out CallableDescriptor>? {
    return this?.getParentCall(context, strict)?.getResolvedCall(context)
}

public fun JetElement.getCallWithAssert(context: BindingContext): Call {
    return getCall(context).sure("No call for ${this.getTextWithLocation()}")
}

public fun JetElement.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure("No resolved call for ${this.getTextWithLocation()}")
}

public fun Call.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure("No resolved call for ${this.getCallElement().getTextWithLocation()}")
}

public fun JetExpression.getFunctionResolvedCallWithAssert(context: BindingContext): ResolvedCall<out FunctionDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.getResultingDescriptor() is FunctionDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends FunctionDescriptor>: ${this.getTextWithLocation()}"
    }
    [suppress("UNCHECKED_CAST")]
    return resolvedCall as ResolvedCall<out FunctionDescriptor>
}

public fun Call.hasUnresolvedArguments(context: BindingContext): Boolean {
    val arguments = getValueArguments().map { it?.getArgumentExpression() }
    return arguments.any {
        argument ->
        val expressionType = context[BindingContext.EXPRESSION_TYPE, argument]
        argument != null && !ArgumentTypeResolver.isFunctionLiteralArgument(argument)
            && (expressionType == null || expressionType.isError())
    }
}

public fun JetReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getParentByType(javaClass<JetDeclarationWithBody>())]
    val containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, javaClass<FunctionDescriptor>(), false)
    if (containingFunctionDescriptor == null) return null

    return stream(containingFunctionDescriptor) { DescriptorUtils.getParentOfType(it, javaClass<FunctionDescriptor>()) }
            .dropWhile { it is AnonymousFunctionDescriptor }
            .firstOrNull()
}

public fun JetExpression.isUsedAsExpression(context: BindingContext): Boolean = context[BindingContext.USED_AS_EXPRESSION, this]!!
public fun JetExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)
