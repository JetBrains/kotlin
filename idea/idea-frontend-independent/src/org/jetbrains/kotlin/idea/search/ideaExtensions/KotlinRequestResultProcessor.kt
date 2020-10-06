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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.ReferenceRange
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.isCallableOverrideUsage
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.isExtensionOfDeclarationClassUsage
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.isUsageInContainingDeclaration
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KotlinRequestResultProcessor(
    private val unwrappedElement: PsiElement,
    private val originalElement: PsiElement = unwrappedElement,
    private val filter: (PsiReference) -> Boolean = { true },
    private val options: KotlinReferencesSearchOptions = KotlinReferencesSearchOptions.Empty
) : RequestResultProcessor(unwrappedElement, originalElement, filter, options) {
    private val referenceService = PsiReferenceService.getService()

    override fun processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Processor<in PsiReference>): Boolean {
        val references = if (element is KtDestructuringDeclaration)
            element.entries.flatMap { referenceService.getReferences(it, PsiReferenceService.Hints.NO_HINTS) }
        else
            referenceService.getReferences(element, PsiReferenceService.Hints.NO_HINTS)
        return references.all { ref ->
            ProgressManager.checkCanceled()

            if (filter(ref) && ref.containsOffsetInElement(offsetInElement) && ref.isReferenceToTarget(unwrappedElement)) {
                consumer.process(ref)
            } else {
                true
            }
        }
    }

    private fun PsiReference.containsOffsetInElement(offsetInElement: Int): Boolean {
        if (this is KtDestructuringDeclarationReference) return true
        return ReferenceRange.containsOffsetInElement(this, offsetInElement)
    }

    private fun PsiReference.isReferenceToTarget(element: PsiElement): Boolean {
        if (isReferenceTo(element)) {
            return true
        }
        if (originalElement is KtNamedDeclaration) {
            if (options.acceptCallableOverrides && isCallableOverrideUsage(originalElement)) {
                return true
            }
            if (options.acceptOverloads && isUsageInContainingDeclaration(originalElement)) {
                return true
            }
            if (options.acceptExtensionsOfDeclarationClass && isExtensionOfDeclarationClassUsage(originalElement)) {
                return true
            }
        }
        return false
    }
}