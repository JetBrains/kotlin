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
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetOperationExpression
import org.jetbrains.jet.lang.resolve.BindingContext.CALL
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext.LABEL_TARGET
import org.jetbrains.jet.lang.resolve.BindingContext.FUNCTION
import org.jetbrains.jet.lang.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor

/**
 *  For expressions like <code>a(), a[i], a.b.c(), +a, a + b, (a()), a(): Int, @label a()</code>
 *  returns a corresponding call.
 *
 *  Note: special construction like <code>a!!, a ?: b, if (c) a else b</code> are resolved as calls,
 *  so there is a corresponding call for them.
 */
fun JetExpression.getCorrespondingCall(bindingContext: BindingContext): Call? {
    val expr = JetPsiUtil.deparenthesize(this)
    if (expr == null) return null

    if (expr is JetQualifiedExpression) {
        return expr.getSelectorExpression()?.getCorrespondingCall(bindingContext)
    }
    val reference = when (expr) {
        is JetCallExpression -> expr.getCalleeExpression()
        is JetOperationExpression -> expr.getOperationReference()
        else -> expr
    }
    return bindingContext[CALL, reference]
}

public fun JetReturnExpression.getTargetFunctionDescriptor(bindingContext: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return bindingContext[LABEL_TARGET, targetLabel]?.let { bindingContext[FUNCTION, it] }

    val declarationDescriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, getParentByType(javaClass<JetDeclarationWithBody>())]
    val containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, javaClass<FunctionDescriptor>(), false)
    if (containingFunctionDescriptor == null) return null

    return stream(containingFunctionDescriptor) { DescriptorUtils.getParentOfType(it, javaClass<FunctionDescriptor>()) }
            .dropWhile { it is AnonymousFunctionDescriptor }
            .firstOrNull()
}
