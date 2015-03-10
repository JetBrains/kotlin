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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class InsertExplicitTypeArguments : JetSelfTargetingIntention<JetCallExpression>(
        "insert.explicit.type.arguments", javaClass()) {

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetCallExpression, editor: Editor): Boolean {
        if (!element.getTypeArguments().isEmpty()) return false
        if (element.getText() == null) return false

        val textRange = element.getCalleeExpression()?.getTextRange()
        if (textRange == null || !textRange.contains(editor.getCaretModel().getOffset())) return false

        val context = element.analyze()
        val resolvedCall = element.getResolvedCall(context)
        if (resolvedCall == null) return false

        val types = resolvedCall.getTypeArguments()
        return !types.isEmpty() && types.values().none { ErrorUtils.containsErrorType(it) }
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val argumentList = createTypeArguments(element)
        if (argumentList == null) return

        val callee = element.getCalleeExpression()
        if (callee == null) return

        element.addAfter(argumentList, callee)
        ShortenReferences.DEFAULT.process(element.getTypeArgumentList()!!)
    }

    default object {
        fun createTypeArguments(element: JetCallExpression): JetTypeArgumentList? {
            val context = element.analyze()
            val resolvedCall = element.getResolvedCall(context)
            if (resolvedCall == null) return null

            val args = resolvedCall.getTypeArguments()
            val types = resolvedCall.getCandidateDescriptor().getTypeParameters()

            val psiFactory = JetPsiFactory(element)
            val typeArgs = types.map {
                assert(args[it] != null, "there is a null in the type arguments to transform")
                IdeDescriptorRenderers.SOURCE_CODE.renderType(args[it]!!)
            }.joinToString(", ", "<", ">")

            return psiFactory.createTypeArguments(typeArgs)
        }
    }
}
