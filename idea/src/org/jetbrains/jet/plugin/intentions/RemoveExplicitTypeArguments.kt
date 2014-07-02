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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.psi.JetTypeProjection
import org.jetbrains.jet.lang.psi.Call
import java.util.ArrayList
import org.jetbrains.jet.di.InjectorForMacros
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetTypeArgumentList
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getResolvedCall
import org.jetbrains.jet.lang.psi.psiUtil.getTextWithLocation

public class RemoveExplicitTypeArguments : JetSelfTargetingIntention<JetTypeArgumentList>(
        "remove.explicit.type.arguments", javaClass()) {

    override fun isApplicableTo(element: JetTypeArgumentList): Boolean {
        val callExpression = element.getParent()
        if (callExpression !is JetCallExpression) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(callExpression)
        if (callExpression.getTypeArguments().isEmpty()) return false

        val resolveSession = callExpression.getLazyResolveSession()
        val injector = InjectorForMacros(callExpression.getProject(), resolveSession.getModuleDescriptor())

        val scope = context[BindingContext.RESOLUTION_SCOPE, callExpression]
        val originalCall = callExpression.getResolvedCall(context)
        if (originalCall == null || scope !is JetScope) return false
        val untypedCall = CallWithoutTypeArgs(originalCall.getCall())

        // todo Check with expected type for other expressions
        // If always use expected type from trace there is a problem with nested calls:
        // the expression type for them can depend on their explicit type arguments (via outer call),
        // therefore we should resolve outer call with erased type arguments for inner call
        val parent = callExpression.getParent()
        val expectedTypeIsExplicitInCode = when (parent) {
            is JetProperty -> parent.getInitializer() == callExpression && parent.getTypeRef() != null
            is JetDeclarationWithBody -> parent.getBodyExpression() == callExpression
            is JetReturnExpression -> true
            else -> false
        }
        val jType = if (expectedTypeIsExplicitInCode) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, callExpression] ?: TypeUtils.NO_EXPECTED_TYPE
        }
        else {
            TypeUtils.NO_EXPECTED_TYPE
        }
        val dataFlow = context[BindingContext.EXPRESSION_DATA_FLOW_INFO, callExpression] ?: DataFlowInfo.EMPTY
        val resolutionResults = injector.getExpressionTypingServices()?.getCallResolver()?.resolveFunctionCall(
                BindingTraceContext(), scope, untypedCall, jType, dataFlow, false)
        assert (resolutionResults?.isSingleResult() ?: true) { "Removing type arguments changed resolve for: " +
                "${callExpression.getTextWithLocation()} to ${resolutionResults?.getResultCode()}" }

        val args = originalCall.getTypeArguments()
        val newArgs = resolutionResults?.getResultingCall()?.getTypeArguments()

        return args == newArgs
    }

    class CallWithoutTypeArgs(call: Call) : DelegatingCall(call) {

        override fun getTypeArguments(): MutableList<JetTypeProjection> {
            return ArrayList<JetTypeProjection>()
        }

        override fun getTypeArgumentList() = null

    }

    override fun applyTo(element: JetTypeArgumentList, editor: Editor) {
        element.delete()
    }
}