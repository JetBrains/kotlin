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

package org.jetbrains.kotlin.idea.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.KotlinCompletionCharFilter
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent

class KotlinFilePathReferenceContributor : AbstractKotlinReferenceContributor() {
    object KotlinFilePathReferenceProvider : FilePathReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
            if (element !is KtStringTemplateExpression) return PsiReference.EMPTY_ARRAY
            if (!element.isPlain()) return PsiReference.EMPTY_ARRAY
            val refByElem = getReferencesByElement(element, element.plainContent, element.getContentRange().startOffset, true)
            val res = refByElem
                .map {
                    if (it is FileReference) {
                        object : FileReference(it.fileReferenceSet, it.rangeInElement, it.index, it.text) {
                            override fun getVariants(): Array<out Any> {
                                return super.getVariants()
                                    .map {
                                        (it as? LookupElement)?.apply {
                                            putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
                                        } ?: it
                                    }
                                    .toTypedArray()
                            }

                            // Note: this is a hack that prevents IDE from renaming paths in string literals when doing move.
                            // TODO: find another way of doing this (that may need re-implementing move files refactoring using .java move)
                            override fun getLastFileReference() = null

                            override fun bindToElement(element: PsiElement): PsiElement {
                                return rename(element.text)
                            }

                            override fun bindToElement(element: PsiElement, absolute: Boolean): PsiElement {
                                return rename(element.text)
                            }
                        }
                    } else it
                }
                .toTypedArray()
            return res
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
            KotlinFilePathReferenceProvider
        )
    }
}
