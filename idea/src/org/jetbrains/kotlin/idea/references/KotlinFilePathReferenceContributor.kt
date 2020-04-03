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
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.KotlinCompletionCharFilter
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent

class KotlinFilePathReferenceContributor : PsiReferenceContributor() {
    object KotlinFilePathReferenceProvider : FilePathReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
            if (element !is KtStringTemplateExpression) return PsiReference.EMPTY_ARRAY
            if (!element.isPlain()) return PsiReference.EMPTY_ARRAY
            val refByElem = getReferencesByElement(element, element.plainContent, element.getContentRange().startOffset, true)
            return refByElem.map { if (it is FileReference) FixedFileReference(it) else it }.toTypedArray()
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
            KotlinFilePathReferenceProvider,
        )
    }
}

private class FixedFileReference(fileReference: FileReference) : FileReference(fileReference) {
    override fun getVariants(): Array<out Any> {
        val variants = super.getVariants()

        variants.forEach {
            if (it is LookupElement) {
                it.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
            }
        }

        return variants
    }

    /**
     * We have a bug that turns file references in code like `"."` and `".."` into `""` during file move because .kt files
     * are moved by [org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinAwareMoveFilesOrDirectoriesProcessor],
     * which is an extender of [com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor]. It uses
     * this method to obtain the referenced file, and if it's null, it will not try to replace the reference to it.
     *
     * However, this is a hack, because there are probably some usages where this reference should not be null.
     *
     * TODO Try to resolve this issue again when IDEA-232942 is resolved (maybe this won't be an issue anymore)
     */
    override fun getLastFileReference(): FileReference? = null
}
