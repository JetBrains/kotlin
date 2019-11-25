/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

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
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.handlers.isCharAt
import org.jetbrains.kotlin.idea.core.ExpectedInfos
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.util.getValueParametersCountFromFunctionType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

object LambdaSignatureTemplates {
    fun insertTemplate(
            context: InsertionContext,
            placeholderRange: TextRange,
            lambdaType: KotlinType,
            explicitParameterTypes: Boolean,
            signatureOnly: Boolean
    ) {
        // we start template later to not interfere with insertion of tail type
        val commandProcessor = CommandProcessor.getInstance()
        val commandName = commandProcessor.currentCommandName ?: "Insert lambda template"
        val commandGroupId = commandProcessor.currentCommandGroupId

        val rangeMarker = context.document.createRangeMarker(placeholderRange)

        context.setLaterRunnable {
            context.project.executeWriteCommand(commandName, groupId = commandGroupId) {
                try {
                    if (rangeMarker.isValid) {
                        val startOffset = rangeMarker.startOffset
                        context.document.deleteString(startOffset, rangeMarker.endOffset)

                        if (signatureOnly) {
                            val spaceAhead = context.document.charsSequence.isCharAt(startOffset, ' ')
                            if (!spaceAhead) {
                                context.document.insertString(startOffset, " ")
                            }
                        }

                        context.editor.caretModel.moveToOffset(startOffset)
                        val template = buildTemplate(lambdaType, signatureOnly, explicitParameterTypes, context.project)
                        TemplateManager.getInstance(context.project).startTemplate(context.editor, template)
                    }
                }
                finally {
                    rangeMarker.dispose()
                }
            }
        }
    }

    val DEFAULT_LAMBDA_PRESENTATION = "{...}"

    enum class SignaturePresentation {
        NAMES,
        NAMES_OR_TYPES,
        NAMES_AND_TYPES
    }

    fun lambdaPresentation(lambdaType: KotlinType, presentationKind: SignaturePresentation): String {
        return "{ " + signaturePresentation(lambdaType, presentationKind) + " ... }"
    }

    fun signaturePresentation(lambdaType: KotlinType, presentationKind: SignaturePresentation): String {
        fun typePresentation(type: KotlinType) = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)

        fun parameterPresentation(parameterType: KotlinType): String {
            val name = parameterType.extractParameterNameFromFunctionTypeArgument()?.render()
            return when (presentationKind) {
                SignaturePresentation.NAMES -> name ?: nameSuggestion(parameterType)
                SignaturePresentation.NAMES_OR_TYPES -> name ?: typePresentation(parameterType)
                SignaturePresentation.NAMES_AND_TYPES -> "${name ?: nameSuggestion(parameterType)}: ${typePresentation(parameterType)}"
            }
        }

        return functionParameterTypes(lambdaType).joinToString(", ", transform = ::parameterPresentation) + " ->"
    }

    fun explicitParameterTypesRequired(file: KtFile, placeholderRange: TextRange, lambdaType: KotlinType): Boolean {
        PsiDocumentManager.getInstance(file.project).commitAllDocuments()
        val expression = PsiTreeUtil.findElementOfClassAtRange(file, placeholderRange.startOffset, placeholderRange.endOffset, KtExpression::class.java)
                         ?: return false

        val resolutionFacade = file.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
        val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade, indicesHelper = null, useHeuristicSignatures = false).calculate(expression)
        val functionTypes = expectedInfos
                .mapNotNull { it.fuzzyType?.type }
                .filter(KotlinType::isFunctionOrSuspendFunctionType)
                .toSet()
        return explicitParameterTypesRequired(functionTypes, lambdaType)
    }

    fun explicitParameterTypesRequired(expectedFunctionTypes: Set<KotlinType>, lambdaType: KotlinType): Boolean {
        if (expectedFunctionTypes.size <= 1) return false
        val lambdaParameterCount = getValueParametersCountFromFunctionType(lambdaType)
        return expectedFunctionTypes.filter { getValueParametersCountFromFunctionType(it) == lambdaParameterCount }.size > 1
    }

    private val TYPE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
        modifiers -= DescriptorRendererModifier.ANNOTATIONS
    }

    private fun buildTemplate(
            lambdaType: KotlinType,
            signatureOnly: Boolean,
            explicitParameterTypes: Boolean,
            project: Project
    ): Template {
        val parameterTypes = functionParameterTypes(lambdaType)

        val manager = TemplateManager.getInstance(project)

        val template = manager.createTemplate("", "")
        template.isToShortenLongNames = true
        //template.setToReformat(true) //TODO
        if (!signatureOnly) {
            template.addTextSegment("{ ")
        }

        val noNameParameterCount = mutableMapOf<KotlinType, Int>()
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
                val count = (noNameParameterCount[parameterType] ?: 0) + 1
                noNameParameterCount[parameterType] = count
                val suffix = if (count == 1) null else "$count"
                val nameSuggestions = nameSuggestions(parameterType, suffix)
                object : Expression() {
                    override fun calculateResult(context: ExpressionContext?) = TextResult(nameSuggestions[0])
                    override fun calculateQuickResult(context: ExpressionContext?): Result? = null
                    override fun calculateLookupItems(context: ExpressionContext?)
                            = nameSuggestions.map { LookupElementBuilder.create(it) }.toTypedArray()
                }
            }
            template.addVariable(nameExpression, true)

            if (explicitParameterTypes) {
                template.addTextSegment(": " + TYPE_RENDERER.renderType(parameterType))
            }
        }

        template.addTextSegment(" -> ")
        template.addEndVariable()

        if (!signatureOnly) {
            template.addTextSegment(" }")
        }

        return template
    }

    private fun nameSuggestions(parameterType: KotlinType, suffix: String? = null): List<String> {
        val suggestions = KotlinNameSuggester.suggestNamesByType(parameterType, { true }, "p")
        return if (suffix != null) suggestions.map { "$it$suffix" } else suggestions
    }
    
    private fun nameSuggestion(parameterType: KotlinType) = nameSuggestions(parameterType)[0]

    private fun functionParameterTypes(functionType: KotlinType): List<KotlinType> {
        return functionType.getValueParameterTypesFromFunctionType().map(TypeProjection::getType)
    }
}
