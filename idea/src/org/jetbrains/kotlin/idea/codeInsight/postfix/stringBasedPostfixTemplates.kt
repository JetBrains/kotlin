/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight.postfix

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.liveTemplates.macro.SuggestVariableNameMacro
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isIterator
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class KtForEachPostfixTemplate(
        name: String
) : StringBasedPostfixTemplate(name, "for (item in expr)", createExpressionSelector(statementsOnly = true, predicate = KotlinType::containsIteratorMethod)) {
    override fun getTemplateString(element: PsiElement) = "for (\$name$ in \$expr$) {\n    \$END$\n}"

    override fun setVariables(template: Template, element: PsiElement) {
        val name = MacroCallNode(SuggestVariableNameMacro())
        template.addVariable("name", name, ConstantNode("item"), true)
    }

    override fun getElementToRemove(expr: PsiElement?) = expr
}

private fun KotlinType.containsIteratorMethod() =
    memberScope.getContributedFunctions(OperatorNameConventions.ITERATOR, NoLookupLocation.FROM_IDE).any {
        it.returnType?.isIterator() ?: false && it.valueParameters.isEmpty()
    }
