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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.ResolvableCollisionUsageInfo
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

abstract class KtResolvableCollisionUsageInfo(
        element: PsiElement,
        referencedElement: PsiElement
) : ResolvableCollisionUsageInfo(element, referencedElement) {
    // To prevent simple rename via PsiReference
    override fun getReference() = null

    abstract fun apply()
}

class UsageInfoWithReplacement(
        element: PsiElement,
        referencedElement: PsiElement,
        private val replacement: KtElement
) : KtResolvableCollisionUsageInfo(element, referencedElement) {
    override fun apply() {
        element?.replaced(replacement)?.addToShorteningWaitSet(ShortenReferences.Options.ALL_ENABLED)
    }
}

class UsageInfoWithFqNameReplacement(
        element: KtSimpleNameExpression,
        referencedElement: PsiElement,
        private val newFqName: FqName
) : KtResolvableCollisionUsageInfo(element, referencedElement) {
    override fun apply() {
        (element as? KtSimpleNameExpression)?.mainReference?.bindToFqName(newFqName)
    }
}