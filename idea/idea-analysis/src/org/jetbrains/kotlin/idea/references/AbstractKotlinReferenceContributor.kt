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

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtElement

abstract class AbstractKotlinReferenceContributor : PsiReferenceContributor() {
    protected inline fun <reified E : KtElement> PsiReferenceRegistrar.registerProvider(
            priority: Double = PsiReferenceRegistrar.DEFAULT_PRIORITY,
            crossinline factory: (E) -> PsiReference?
    ) {
        registerMultiProvider<E>(priority) { factory(it)?.let { arrayOf(it) } ?: PsiReference.EMPTY_ARRAY }
    }

    protected inline fun <reified E : KtElement> PsiReferenceRegistrar.registerMultiProvider(
            priority: Double = PsiReferenceRegistrar.DEFAULT_PRIORITY,
            crossinline factory: (E) -> Array<PsiReference>
    ) {
        registerMultiProvider(PlatformPatterns.psiElement(E::class.java), priority, factory)
    }

    protected inline fun <E : KtElement> PsiReferenceRegistrar.registerMultiProvider(
            pattern: ElementPattern<E>,
            priority: Double = PsiReferenceRegistrar.DEFAULT_PRIORITY,
            crossinline factory: (E) -> Array<PsiReference>
    ) {
        registerReferenceProvider(
                pattern,
                object : PsiReferenceProvider() {
                    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                        @Suppress("UNCHECKED_CAST")
                        return factory(element as E)
                    }
                },
                priority
        )
    }
}