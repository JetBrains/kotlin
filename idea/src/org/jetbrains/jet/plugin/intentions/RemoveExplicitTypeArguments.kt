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
import org.jetbrains.jet.lang.psi.JetPsiFactory
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

public class RemoveExplicitTypeArguments : JetSelfTargetingIntention<JetCallExpression>(
        "remove.explicit.type.arguments", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean {

        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        if (element.getTypeArguments().isEmpty()) return false

        val resolveSession = element.getLazyResolveSession()
        val injector = InjectorForMacros(element.getProject(), resolveSession.getModuleDescriptor())

        val scope = context[BindingContext.RESOLUTION_SCOPE, element]
        val originalCall = context[BindingContext.RESOLVED_CALL, element.getCalleeExpression()]?.getCall()
        if (originalCall == null || scope !is JetScope) return false
        val untypedCall = CallWithoutTypeArgs(originalCall)

        val jType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, element] ?: TypeUtils.NO_EXPECTED_TYPE
        val dataFlow = context[BindingContext.EXPRESSION_DATA_FLOW_INFO, element] ?: DataFlowInfo.EMPTY
        val resolvedCall = injector.getExpressionTypingServices()?.getCallResolver()?.resolveFunctionCall(
                BindingTraceContext(), scope, untypedCall, jType, dataFlow, false)

        val args = context[BindingContext.RESOLVED_CALL, element.getCalleeExpression()]?.getTypeArguments()
        val newArgs = resolvedCall?.getResultingCall()?.getTypeArguments()

        return args == newArgs
    }

    class CallWithoutTypeArgs(call: Call) : DelegatingCall(call) {

        override fun getTypeArguments(): MutableList<JetTypeProjection> {
            return ArrayList<JetTypeProjection>()
        }

        override fun getTypeArgumentList() = null

    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val text = element.getText()
        val typeArgs = element.getTypeArgumentList()
        if (text == null || typeArgs == null) return
        val base = typeArgs.getTextOffset() - element.getTextOffset()
        val untypedText = "${text.substring(0, base)}${text.substring(base + typeArgs.getTextLength())}"
        element.replace(JetPsiFactory.createExpression(element.getProject(), untypedText))
    }
}



