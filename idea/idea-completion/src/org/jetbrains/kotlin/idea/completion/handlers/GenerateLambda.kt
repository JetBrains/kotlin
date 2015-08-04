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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.ExpectedInfos
import org.jetbrains.kotlin.idea.completion.fuzzyType
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.JetType

fun insertLambdaTemplate(context: InsertionContext, placeholderRange: TextRange, lambdaType: JetType) {
    val explicitParameterTypes = needExplicitParameterTypes(context, placeholderRange, lambdaType)

    // we start template later to not interfere with insertion of tail type
    val commandProcessor = CommandProcessor.getInstance()
    val commandName = commandProcessor.getCurrentCommandName()!!
    val commandGroupId = commandProcessor.getCurrentCommandGroupId()

    val rangeMarker = context.getDocument().createRangeMarker(placeholderRange)

    context.setLaterRunnable {
        context.getProject().executeWriteCommand(commandName, groupId = commandGroupId) {
            try {
                if (rangeMarker.isValid()) {
                    context.getDocument().deleteString(rangeMarker.getStartOffset(), rangeMarker.getEndOffset())
                    context.getEditor().getCaretModel().moveToOffset(rangeMarker.getStartOffset())
                    val template = buildTemplate(lambdaType, explicitParameterTypes, context.getProject())
                    TemplateManager.getInstance(context.getProject()).startTemplate(context.getEditor(), template)
                }
            }
            finally {
                rangeMarker.dispose()
            }
        }
    }
}

fun buildLambdaPresentation(lambdaType: JetType): String {
    val parameterTypes = functionParameterTypes(lambdaType)
    val parametersPresentation = parameterTypes.map { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }.joinToString(", ")
    return "{ $parametersPresentation -> ... }"
}

private fun needExplicitParameterTypes(context: InsertionContext, placeholderRange: TextRange, lambdaType: JetType): Boolean {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments()
    val file = context.getFile() as JetFile
    val expression = PsiTreeUtil.findElementOfClassAtRange(file, placeholderRange.getStartOffset(), placeholderRange.getEndOffset(), javaClass<JetExpression>())
                     ?: return false

    val resolutionFacade = file.getResolutionFacade()
    val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
    val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade, resolutionFacade.findModuleDescriptor(file), useHeuristicSignatures = false)
                                .calculate(expression) ?: return false
    val functionTypes = expectedInfos
            .map { it.fuzzyType?.type }
            .filterNotNull()
            .filter { KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(it) }
            .toSet()
    if (functionTypes.size() <= 1) return false

    val lambdaParameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(lambdaType).size()
    return functionTypes.filter { KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(it).size() == lambdaParameterCount }.size() > 1
}

private fun buildTemplate(lambdaType: JetType, explicitParameterTypes: Boolean, project: Project): Template {
    val parameterTypes = functionParameterTypes(lambdaType)

    val manager = TemplateManager.getInstance(project)

    val template = manager.createTemplate("", "")
    template.setToShortenLongNames(true)
    //template.setToReformat(true) //TODO
    template.addTextSegment("{ ")

    for ((i, parameterType) in parameterTypes.withIndex()) {
        if (i > 0) {
            template.addTextSegment(", ")
        }
        //TODO: check for names in scope
        template.addVariable(ParameterNameExpression(KotlinNameSuggester.suggestNamesByType(parameterType, { true }, "p").toTypedArray()), true)
        if (explicitParameterTypes) {
            template.addTextSegment(": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(parameterType))
        }
    }

    template.addTextSegment(" -> ")
    template.addEndVariable()
    template.addTextSegment(" }")
    return template
}

private class ParameterNameExpression(val nameSuggestions: Array<String>) : Expression() {
    override fun calculateResult(context: ExpressionContext?) = TextResult(nameSuggestions[0])

    override fun calculateQuickResult(context: ExpressionContext?): Result? = null

    override fun calculateLookupItems(context: ExpressionContext?)
            = Array<LookupElement>(nameSuggestions.size(), { LookupElementBuilder.create(nameSuggestions[it]) })
}

fun functionParameterTypes(functionType: JetType): List<JetType>
        = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(functionType).map { it.getType() }
