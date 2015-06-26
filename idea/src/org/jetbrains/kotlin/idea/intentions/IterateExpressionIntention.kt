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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.IterableTypesDetector
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.JetType

public class IterateExpressionIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Iterate over collection"), HighPriorityAction {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        if (element.getParent() !is JetBlockExpression) return false
        val range = element.getTextRange()
        if (caretOffset != range.getStartOffset() && caretOffset != range.getEndOffset()) return false
        val data = data(element) ?: return false
        setText("Iterate over '${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(data.collectionType)}'")
        return true
    }

    private data class Data(val collectionType: JetType, val elementType: JetType)

    private fun data(expression: JetExpression): Data? {
        val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
        val type = bindingContext.getType(expression) ?: return null
        val moduleDescriptor = expression.getResolutionFacade().findModuleDescriptor(expression)
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression]
        val elementType = IterableTypesDetector(expression.getProject(), moduleDescriptor, scope).elementType(type)?.type ?: return null
        return Data(type, elementType)
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        //TODO: multi-declaration (when?)

        val elementType = data(element)!!.elementType
        //TODO: base on expression too
        //TODO: name validation
        val names = KotlinNameSuggester.suggestNames(elementType, { true }, "e")

        var forExpression = JetPsiFactory(element).createExpressionByPattern("for($0 in $1) {\nx\n}", names[0], element) as JetForExpression
        forExpression = element.replaced(forExpression)

        PsiDocumentManager.getInstance(forExpression.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument())

        val bodyPlaceholder = (forExpression.getBody() as JetBlockExpression).getStatements().single()

        val templateBuilder = TemplateBuilderImpl(forExpression)
        templateBuilder.replaceElement(forExpression.getLoopParameter()!!, ChooseStringExpression(names.asList()))
        templateBuilder.replaceElement(bodyPlaceholder, ConstantNode(""), false)
        templateBuilder.setEndVariableAfter(bodyPlaceholder)

        templateBuilder.run(editor, true)
    }
}