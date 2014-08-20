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

package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.psi.JetValueArgument
import com.intellij.codeInsight.completion.CompletionParameters
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetCallElement
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.filters.AndFilter
import org.jetbrains.jet.lang.psi.JetValueArgumentName
import com.intellij.psi.filters.position.ParentElementFilter
import com.intellij.psi.filters.OrFilter
import com.intellij.psi.filters.ClassFilter
import org.jetbrains.jet.plugin.util.FirstChildInParentFilter
import org.jetbrains.jet.lang.psi.psiUtil.getCallNameExpression
import com.intellij.psi.PsiElement

object NamedParametersCompletion {
    private val positionFilter = AndFilter(
            LeafElementFilter(JetTokens.IDENTIFIER),
            OrFilter(
                    AndFilter(
                            ParentElementFilter(ClassFilter(javaClass<JetValueArgument>()), 2),
                            FirstChildInParentFilter(2)
                    ),
                    ParentElementFilter(ClassFilter(javaClass<JetValueArgumentName>()), 2)
            )
    )

    public fun isOnlyNamedParameterExpected(position: PsiElement): Boolean {
        if (!positionFilter.isAcceptable(position, position)) return false

        val thisArgument = PsiTreeUtil.getParentOfType(position, javaClass<JetValueArgument>())!!

        val callElement = PsiTreeUtil.getParentOfType(thisArgument, javaClass<JetCallElement>()) ?: return false

        for (argument in callElement.getValueArguments()) {
            if (argument.isNamed()) return true
            if (argument == thisArgument) break
        }

        return false
    }

    public fun complete(position: PsiElement, collector: LookupElementsCollector) {
        if (!positionFilter.isAcceptable(position, position)) return

        val valueArgument = PsiTreeUtil.getParentOfType(position, javaClass<JetValueArgument>())!!

        val callElement = PsiTreeUtil.getParentOfType(valueArgument, javaClass<JetCallElement>()) ?: return
        val callSimpleName = callElement.getCallNameExpression() ?: return

        val callReference = callSimpleName.getReference() as JetReference

        val functionDescriptors = callReference.resolveToDescriptors().map { it as? FunctionDescriptor }.filterNotNull()

        for (funDescriptor in functionDescriptors) {
            if (!funDescriptor.hasStableParameterNames()) continue

            val usedArguments = QuickFixUtil.getUsedParameters(callElement, valueArgument, funDescriptor)

            for (parameter in funDescriptor.getValueParameters()) {
                val name = parameter.getName().asString()
                if (name !in usedArguments) {
                    val lookupElement = LookupElementBuilder.create("$name")
                            .withPresentableText("$name = ")
                            .withTailText("${DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(parameter.getType())}")
                            .withIcon(JetIcons.PARAMETER)
                            .withInsertHandler(NamedParameterInsertHandler)
                            .assignPriority(ItemPriority.NAMED_PARAMETER)
                    lookupElement.putUserData(JetCompletionCharFilter.ACCEPT_EQ, true);

                    collector.addElement(lookupElement)
                }
            }
        }
    }

    private object NamedParameterInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val ch = context.getCompletionChar()
            if (ch == '=' || ch == ' ') {
                context.setAddCompletionChar(false)
            }

            val editor = context.getEditor()
            val tailOffset = context.getTailOffset()

            editor.getDocument().insertString(tailOffset, " = ")
            editor.getCaretModel().moveToOffset(tailOffset + 3)
        }
    }
}