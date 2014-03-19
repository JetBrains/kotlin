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

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.jet.lang.psi.JetValueArgument
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.util.ProcessingContext
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetCallElement
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.plugin.completion.KotlinNamedParametersContributor.NamedParameterLookupObject
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.plugin.completion.weigher.addJetSorting
import org.jetbrains.jet.lang.psi.psiUtil.getCallSimpleNameExpression
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.filters.AndFilter
import org.jetbrains.jet.lang.psi.JetValueArgumentName
import com.intellij.psi.filters.position.ParentElementFilter
import com.intellij.psi.filters.OrFilter
import com.intellij.psi.filters.ClassFilter
import org.jetbrains.jet.plugin.util.FirstChildInParentFilter

public class KotlinNamedParametersContributor : CompletionContributor() {
    public class NamedParameterLookupObject(val name: Name) {}

    private val InNamedParameterFilter = AndFilter(
            LeafElementFilter(JetTokens.IDENTIFIER),
            OrFilter(
                    AndFilter(
                            ParentElementFilter(ClassFilter(javaClass<JetValueArgument>()), 2),
                            FirstChildInParentFilter(2)),
                    ParentElementFilter(ClassFilter(javaClass<JetValueArgumentName>()), 2)
            )
    );

    {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement().and(FilterPattern(InNamedParameterFilter)),
               object : CompletionProvider<CompletionParameters>() {
                   override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
                       doParamsCompletion(parameters, result)
                   }
               })
    }

    fun doParamsCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val valueArgument = PsiTreeUtil.getParentOfType(parameters.getPosition(), javaClass<JetValueArgument>())!!

        val callElement = PsiTreeUtil.getParentOfType(valueArgument, javaClass<JetCallElement>())
        if (callElement == null) return

        val callSimpleName = getCallSimpleNameExpression(callElement)
        if (callSimpleName == null) return

        val kotlinResultSet = result.addJetSorting(parameters)

        val callReference = callSimpleName.getReference() as JetReference

        val functionDescriptors = callReference.resolveToDescriptors().map { it as? FunctionDescriptor }.filterNotNull()

        for (funDescriptor in functionDescriptors) {
            val usedArguments = QuickFixUtil.getUsedParameters(callElement, valueArgument, funDescriptor)

            for (parameter in funDescriptor.getValueParameters()) {
                val name = parameter.getName().asString()
                if (result.getPrefixMatcher().prefixMatches(name) && name !in usedArguments) {
                    val lookupElementBuilder = LookupElementBuilder.create(NamedParameterLookupObject(parameter.getName()), "${name}")
                            .withPresentableText("${name} = ")
                            .withTailText("${DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(parameter.getType())}")
                            .withIcon(JetIcons.PARAMETER)
                            .withInsertHandler(NamedParameterInsertHandler)

                    kotlinResultSet.addElement(lookupElementBuilder)
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