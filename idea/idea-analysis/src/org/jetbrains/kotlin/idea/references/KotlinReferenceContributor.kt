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

package org.jetbrains.kotlin.idea.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KotlinReferenceContributor() : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(KtSimpleNameExpression::class.java) {
                KtSimpleNameReference(it)
            }

            registerMultiProvider(KtNameReferenceExpression::class.java) {
                if (it.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMultiProvider emptyArray()

                when (it.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Getter(it))
                    ReferenceAccess.WRITE ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Setter(it))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Getter(it), SyntheticPropertyAccessorReference.Setter(it))
                }
            }

            registerProvider(KtConstructorDelegationReferenceExpression::class.java) {
                KtConstructorDelegationReference(it)
            }

            registerProvider(KtCallExpression::class.java) {
                KtInvokeFunctionReference(it)
            }

            registerProvider(KtArrayAccessExpression::class.java) {
                KtArrayAccessReference(it)
            }

            registerProvider(KtForExpression::class.java) {
                KtForLoopInReference(it)
            }

            registerProvider(KtPropertyDelegate::class.java) {
                KtPropertyDelegationMethodsReference(it)
            }

            registerProvider(KtDestructuringDeclaration::class.java) {
                KtDestructuringDeclarationReference(it)
            }

            registerProvider(KDocName::class.java) {
                KDocReference(it)
            }
        }
    }

    private fun <E : KtElement> PsiReferenceRegistrar.registerProvider(elementClass: Class<E>, factory: (E) -> KtReference) {
        registerMultiProvider(elementClass, { arrayOf(factory(it)) })
    }

    private fun <E : KtElement> PsiReferenceRegistrar.registerMultiProvider(elementClass: Class<E>, factory: (E) -> Array<PsiReference>) {
        registerReferenceProvider(PlatformPatterns.psiElement(elementClass), object: PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                @Suppress("UNCHECKED_CAST")
                return factory(element as E)
            }
        })
    }
}
