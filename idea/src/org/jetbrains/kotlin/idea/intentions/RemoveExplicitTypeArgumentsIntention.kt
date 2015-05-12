/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.di.InjectorForMacros
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.types.TypeUtils

public class RemoveExplicitTypeArgumentsInspection : IntentionBasedInspection<JetTypeArgumentList>(RemoveExplicitTypeArgumentsIntention()) {
    override val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

public class RemoveExplicitTypeArgumentsIntention : JetSelfTargetingOffsetIndependentIntention<JetTypeArgumentList>(javaClass(), "Remove explicit type arguments") {
    override fun isApplicableTo(element: JetTypeArgumentList): Boolean {
        val callExpression = element.getParent() as? JetCallExpression ?: return false
        if (callExpression.getTypeArguments().isEmpty()) return false

        val context = callExpression.analyze()
        val scope = context[BindingContext.RESOLUTION_SCOPE, callExpression] ?: return false
        val originalCall = callExpression.getResolvedCall(context) ?: return false
        val untypedCall = CallWithoutTypeArgs(originalCall.getCall())

        // todo Check with expected type for other expressions
        // If always use expected type from trace there is a problem with nested calls:
        // the expression type for them can depend on their explicit type arguments (via outer call),
        // therefore we should resolve outer call with erased type arguments for inner call
        val parent = callExpression.getParent()
        val expectedTypeIsExplicitInCode = when (parent) {
            is JetProperty -> parent.getInitializer() == callExpression && parent.getTypeReference() != null
            is JetDeclarationWithBody -> parent.getBodyExpression() == callExpression
            is JetReturnExpression -> true
            else -> false
        }
        val expectedType = if (expectedTypeIsExplicitInCode) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, callExpression] ?: TypeUtils.NO_EXPECTED_TYPE
        }
        else {
            TypeUtils.NO_EXPECTED_TYPE
        }
        val dataFlow = context.getDataFlowInfo(callExpression)
        val injector = InjectorForMacros(callExpression.getProject(), callExpression.findModuleDescriptor())
        val resolutionResults = injector.getCallResolver().resolveFunctionCall(
                BindingTraceContext(), scope, untypedCall, expectedType, dataFlow, false)
        assert (resolutionResults.isSingleResult()) {
            "Removing type arguments changed resolve for: ${callExpression.getTextWithLocation()} to ${resolutionResults.getResultCode()}"
        }

        val args = originalCall.getTypeArguments()
        val newArgs = resolutionResults.getResultingCall().getTypeArguments()

        return args == newArgs.mapValues { approximateFlexibleTypes(it.getValue(), false) }
    }

    private class CallWithoutTypeArgs(call: Call) : DelegatingCall(call) {
        override fun getTypeArguments() = emptyList<JetTypeProjection>()
        override fun getTypeArgumentList() = null
    }

    override fun applyTo(element: JetTypeArgumentList, editor: Editor) {
        element.delete()
    }
}