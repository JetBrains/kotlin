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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.idea.completion.LambdaSignatureTemplates
import org.jetbrains.kotlin.idea.completion.suppressAutoInsertion
import org.jetbrains.kotlin.idea.core.ExpectedInfos
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

object LambdaSignatureItems {
    fun addToCollection(
            collection: MutableCollection<LookupElement>,
            position: KtExpression,
            bindingContext: BindingContext,
            resolutionFacade: ResolutionFacade
    ) {
        val block = position.parent as? KtBlockExpression ?: return
        if (position != block.statements.first()) return
        val functionLiteral = block.parent as? KtFunctionLiteral ?: return
        if (functionLiteral.arrow != null) return
        val literalExpression = functionLiteral.parent as KtLambdaExpression

        val expectedFunctionTypes = ExpectedInfos(bindingContext, resolutionFacade, null).calculate(literalExpression)
                .mapNotNull { it.fuzzyType?.type }
                .filter { it.isFunctionOrSuspendFunctionType }
                .toSet()
        for (functionType in expectedFunctionTypes) {
            if (functionType.getValueParameterTypesFromFunctionType().isEmpty()) continue

            if (LambdaSignatureTemplates.explicitParameterTypesRequired(expectedFunctionTypes, functionType)) {
                collection.add(createLookupElement(functionType, LambdaSignatureTemplates.SignaturePresentation.NAMES_OR_TYPES, explicitParameterTypes = true))
            }
            else {
                collection.add(createLookupElement(functionType, LambdaSignatureTemplates.SignaturePresentation.NAMES, explicitParameterTypes = false))
                collection.add(createLookupElement(functionType, LambdaSignatureTemplates.SignaturePresentation.NAMES_AND_TYPES, explicitParameterTypes = true))
            }
        }
    }

    private fun createLookupElement(
        functionType: KotlinType,
        signaturePresentation: LambdaSignatureTemplates.SignaturePresentation,
        explicitParameterTypes: Boolean
    ): LookupElement {
        val lookupString = LambdaSignatureTemplates.signaturePresentation(functionType, signaturePresentation)
        val priority = if (explicitParameterTypes)
            SmartCompletionItemPriority.LAMBDA_SIGNATURE_EXPLICIT_PARAMETER_TYPES
        else
            SmartCompletionItemPriority.LAMBDA_SIGNATURE
        return LookupElementBuilder.create(lookupString)
            .withInsertHandler { context, _ ->
                val offset = context.startOffset
                val placeholder = "{}"
                context.document.replaceString(offset, context.tailOffset, placeholder)
                LambdaSignatureTemplates.insertTemplate(
                    context,
                    TextRange(offset, offset + placeholder.length),
                    functionType,
                    explicitParameterTypes,
                    signatureOnly = true
                )
            }
            .suppressAutoInsertion()
            .assignSmartCompletionPriority(priority)
    }
}