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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.completion.ExpectedInfo
import org.jetbrains.kotlin.idea.completion.fuzzyType
import org.jetbrains.kotlin.idea.completion.handlers.buildLambdaPresentation
import org.jetbrains.kotlin.idea.completion.handlers.insertLambdaTemplate
import org.jetbrains.kotlin.idea.completion.suppressAutoInsertion
import java.util.ArrayList

object LambdaItems {
    public fun collect(functionExpectedInfos: Collection<ExpectedInfo>): Collection<LookupElement> {
        val list = ArrayList<LookupElement>()
        addToCollection(list, functionExpectedInfos)
        return list
    }

    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        val functionExpectedInfos = expectedInfos.filterFunctionExpected()
        if (functionExpectedInfos.isEmpty()) return

        val distinctTypes = functionExpectedInfos
                .map { it.fuzzyType?.type }
                .filterNotNull()
                .toSet()

        val singleType = if (distinctTypes.size() == 1) distinctTypes.single() else null
        val singleSignatureLength = singleType?.let { KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(it).size() }
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = LookupElementBuilder.create("{...}")
                    .withInsertHandler(ArtificialElementInsertHandler("{ ", " }", false))
                    .suppressAutoInsertion()
                    .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA_NO_PARAMS)
                    .addTailAndNameSimilarity(functionExpectedInfos)
            collection.add(lookupElement)
        }

        if (singleSignatureLength != 0) {
            for (functionType in distinctTypes) {
                val lookupString = buildLambdaPresentation(functionType)
                val lookupElement = LookupElementBuilder.create(lookupString)
                        .withInsertHandler({ context, lookupElement ->
                                               val offset = context.getStartOffset()
                                               val placeholder = "{}"
                                               context.getDocument().replaceString(offset, context.getTailOffset(), placeholder)
                                               insertLambdaTemplate(context, TextRange(offset, offset + placeholder.length()), functionType)
                                           })
                        .suppressAutoInsertion()
                        .assignSmartCompletionPriority(SmartCompletionItemPriority.LAMBDA)
                        .addTailAndNameSimilarity(functionExpectedInfos.filter { it.fuzzyType?.type == functionType })
                collection.add(lookupElement)
            }
        }
    }
}
