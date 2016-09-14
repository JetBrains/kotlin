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
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.ExpectedInfos
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.util.getValueParametersCountFromFunctionType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

fun insertLambdaTemplate(context: InsertionContext, placeholderRange: TextRange, lambdaType: KotlinType) {
    val explicitParameterTypes = needExplicitParameterTypes(context, placeholderRange, lambdaType)

    // we start template later to not interfere with insertion of tail type
    val commandProcessor = CommandProcessor.getInstance()
    val commandName = commandProcessor.currentCommandName!!
    val commandGroupId = commandProcessor.currentCommandGroupId

    val rangeMarker = context.document.createRangeMarker(placeholderRange)

    context.setLaterRunnable {
        context.project.executeWriteCommand(commandName, groupId = commandGroupId) {
            try {
                if (rangeMarker.isValid) {
                    context.document.deleteString(rangeMarker.startOffset, rangeMarker.endOffset)
                    context.editor.caretModel.moveToOffset(rangeMarker.startOffset)
                    val template = buildTemplate(lambdaType, explicitParameterTypes, context.project)
                    TemplateManager.getInstance(context.project).startTemplate(context.editor, template)
                }
            }
            finally {
                rangeMarker.dispose()
            }
        }
    }
}

fun lambdaPresentation(lambdaType: KotlinType?): String {
    if (lambdaType == null) return "{...}"
    val parameterTypes = functionParameterTypes(lambdaType)
    val parametersPresentation = parameterTypes
            .map {
                it.extractParameterNameFromFunctionTypeArgument() ?: IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it)
            }
            .joinToString(", ")
    return "{ $parametersPresentation -> ... }"
}

private fun needExplicitParameterTypes(context: InsertionContext, placeholderRange: TextRange, lambdaType: KotlinType): Boolean {
    PsiDocumentManager.getInstance(context.project).commitAllDocuments()
    val file = context.file as KtFile
    val expression = PsiTreeUtil.findElementOfClassAtRange(file, placeholderRange.startOffset, placeholderRange.endOffset, KtExpression::class.java)
                     ?: return false

    val resolutionFacade = file.getResolutionFacade()
    val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
    val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade, indicesHelper = null, useHeuristicSignatures = false).calculate(expression)

    val functionTypes = expectedInfos
            .mapNotNull { it.fuzzyType?.type }
            .filter(KotlinType::isFunctionType)
            .toSet()
    if (functionTypes.size <= 1) return false

    val lambdaParameterCount = getValueParametersCountFromFunctionType(lambdaType)
    return functionTypes.filter { getValueParametersCountFromFunctionType(it) == lambdaParameterCount }.size > 1
}

private fun buildTemplate(lambdaType: KotlinType, explicitParameterTypes: Boolean, project: Project): Template {
    val parameterTypes = functionParameterTypes(lambdaType)

    val manager = TemplateManager.getInstance(project)

    val template = manager.createTemplate("", "")
    template.isToShortenLongNames = true
    //template.setToReformat(true) //TODO
    template.addTextSegment("{ ")

    for ((i, parameterType) in parameterTypes.withIndex()) {
        if (i > 0) {
            template.addTextSegment(", ")
        }
        //TODO: check for names in scope
        val parameterName = parameterType.extractParameterNameFromFunctionTypeArgument()?.render()
        val nameExpression =  if (parameterName != null) {
            object : Expression() {
                override fun calculateResult(context: ExpressionContext?) = TextResult(parameterName)
                override fun calculateQuickResult(context: ExpressionContext?): Result? = TextResult(parameterName)
                override fun calculateLookupItems(context: ExpressionContext?) = emptyArray<LookupElement>()
            }
        }
        else {
            val nameSuggestions = KotlinNameSuggester.suggestNamesByType(parameterType, { true }, "p").toTypedArray()
            object : Expression() {
                override fun calculateResult(context: ExpressionContext?) = TextResult(nameSuggestions[0])
                override fun calculateQuickResult(context: ExpressionContext?): Result? = null
                override fun calculateLookupItems(context: ExpressionContext?)
                        = nameSuggestions.map { LookupElementBuilder.create(it) }.toTypedArray()
            }
        }
        template.addVariable(nameExpression, true)

        if (explicitParameterTypes) {
            template.addTextSegment(": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(parameterType))
        }
    }

    template.addTextSegment(" -> ")
    template.addEndVariable()
    template.addTextSegment(" }")
    return template
}

private fun functionParameterTypes(functionType: KotlinType): List<KotlinType> {
    return functionType.getValueParameterTypesFromFunctionType().map(TypeProjection::getType)
}
