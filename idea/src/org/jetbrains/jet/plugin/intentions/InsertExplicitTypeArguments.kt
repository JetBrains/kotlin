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
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences

public class InsertExplicitTypeArguments : JetSelfTargetingIntention<JetCallExpression>(
        "insert.explicit.type.arguments", javaClass()) {

    private var resolvedCall: ResolvedCall<out CallableDescriptor?> by Delegates.notNull()

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (!element.getTypeArguments().isEmpty()) return false
        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val nullableResolvedCall = context[BindingContext.RESOLVED_CALL, element.getCalleeExpression()]
        if (nullableResolvedCall == null) return false
        resolvedCall = nullableResolvedCall
        val types = resolvedCall.getTypeArguments()
        return !types.isEmpty() && types.values().none { ErrorUtils.containsErrorType(it) }
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val args = resolvedCall.getTypeArguments()
        val types = resolvedCall.getCandidateDescriptor()?.getTypeParameters()
        if (types == null) return

        val typeArgs = types.map {
            assert(args[it] != null, "there is a null in the type arguments to transform")
            val typeToCompute = DescriptorRenderer.FQNAMES_IN_TYPES.renderType(args[it]!!);
            val computedTypeRef = JetPsiFactory.createType(element.getProject(), typeToCompute)
            ShortenReferences.process(computedTypeRef)
            computedTypeRef.getText()
        }.makeString(", ", "<", ">")

        val name = element.getCalleeExpression()?.getText()
        val text = element.getText()
        if (name == null || text == null) return
        val valueAndFunctionArguments = text.substring(name.size)
        val expr = JetPsiFactory.createExpression(element.getProject(), "$name$typeArgs${valueAndFunctionArguments}")
        element.replace(expr)
    }
}