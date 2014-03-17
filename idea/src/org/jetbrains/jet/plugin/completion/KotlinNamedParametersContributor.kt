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
import org.jetbrains.jet.plugin.parameterInfo.JetFunctionParameterInfoHandler
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.plugin.completion.KotlinNamedParametersContributor.NamedParameterLookupObject
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.plugin.quickfix.AddNameToArgumentFix
import org.jetbrains.jet.plugin.completion.weigher.addJetSorting
import org.jetbrains.jet.lang.psi.psiUtil.getCallSimpleNameExpression
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil

public class KotlinNamedParametersContributor : CompletionContributor() {
    public class NamedParameterLookupObject(val name: Name) {}

    {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement().withElementType(JetTokens.IDENTIFIER).withSuperParent(2, javaClass<JetValueArgument>()),
               object : CompletionProvider<CompletionParameters>() {
                   override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
                       doParamsCompletion(parameters, context, result)
                   }
               })
    }

    fun doParamsCompletion(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        val valueArgument = PsiTreeUtil.getParentOfType(parameters.getPosition(), javaClass<JetValueArgument>())!!

        val callElement = PsiTreeUtil.getParentOfType(valueArgument, javaClass<JetCallElement>())
        if (callElement == null) return

        val callSimpleName = getCallSimpleNameExpression(callElement)
        if (callSimpleName == null) return

        val kotlinResultSet = result.addJetSorting(parameters)

        val callReference = callSimpleName.getReference() as JetReference

        val functionDescriptors = callReference.resolveToDescriptors().iterator()
                .filter { it is FunctionDescriptor }
                .map { it as FunctionDescriptor }

        for (funDescriptor in functionDescriptors) {
            val usedArguments = QuickFixUtil.getUsedParameters(callElement, valueArgument, funDescriptor)

            for (parameter in funDescriptor.getValueParameters()) {
                val name = parameter.getName().asString()
                if (result.getPrefixMatcher().prefixMatches(name) && !usedArguments.contains(name)) {
                    val lookupElementBuilder = LookupElementBuilder.create(NamedParameterLookupObject(parameter.getName()), "${name} = ")
                        .withIcon(JetIcons.PARAMETER)

                    kotlinResultSet.addElement(lookupElementBuilder)
                }
            }
        }
    }
}