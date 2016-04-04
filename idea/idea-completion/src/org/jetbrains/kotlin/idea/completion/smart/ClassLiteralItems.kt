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

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.completion.BasicLookupElementFactory
import org.jetbrains.kotlin.idea.completion.createLookupElementForType
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

object ClassLiteralItems {
    fun addToCollection(
            collection: MutableCollection<LookupElement>,
            expectedInfos: Collection<ExpectedInfo>,
            lookupElementFactory: BasicLookupElementFactory,
            isJvmModule: Boolean
    ) {
        val typeAndSuffixToExpectedInfos = LinkedHashMap<Pair<KotlinType, String>, MutableList<ExpectedInfo>>()

        for (expectedInfo in expectedInfos) {
            val fuzzyType = expectedInfo.fuzzyType ?: continue
            if (fuzzyType.freeParameters.isNotEmpty()) continue
            val typeConstructor = fuzzyType.type.constructor
            val klass = typeConstructor.declarationDescriptor as? ClassDescriptor ?: continue
            val typeArgument = fuzzyType.type.arguments.singleOrNull() ?: continue
            if (typeArgument.projectionKind != Variance.INVARIANT) continue

            if (KotlinBuiltIns.isKClass(klass)) {
                typeAndSuffixToExpectedInfos.getOrPut(typeArgument.type to "::class") { ArrayList() }.add(expectedInfo)
            }

            if (isJvmModule && klass.importableFqName?.asString() == "java.lang.Class") {
                typeAndSuffixToExpectedInfos.getOrPut(typeArgument.type to "::class.java") { ArrayList() }.add(expectedInfo)
            }
        }

        for ((pair, matchedExpectedInfos) in typeAndSuffixToExpectedInfos) {
            val (type, suffix) = pair
            val typeToUse = if (KotlinBuiltIns.isArray(type)) {
                type.makeNotNullable()
            }
            else {
                val classifier = (type.constructor.declarationDescriptor as? ClassDescriptor) ?: continue
                classifier.defaultType
            }

            var lookupElement = lookupElementFactory.createLookupElementForType(typeToUse) ?: continue

            lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText += suffix
                }

                override fun handleInsert(context: InsertionContext) {
                    super.handleInsert(context)

                    PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)

                    val offset = context.tailOffset
                    context.document.insertString(offset, suffix)
                    context.editor.moveCaret(offset + suffix.length)
                }
            }
            lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.CLASS_LITERAL)
            lookupElement = lookupElement.addTailAndNameSimilarity(matchedExpectedInfos, emptyList())
            collection.add(lookupElement)
        }
    }
}