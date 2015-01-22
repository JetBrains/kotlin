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

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import com.intellij.util.ProcessingContext
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName

public class JetReferenceContributor() : PsiReferenceContributor() {
    public override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(javaClass<JetSimpleNameExpression>()) {
                JetSimpleNameReference(it)
            }
            registerProvider(javaClass<JetConstructorDelegationReferenceExpression>()) {
                JetConstructorDelegationReference(it)
            }
            registerProvider(javaClass<JetCallExpression>()) {
                JetInvokeFunctionReference(it)
            }
            registerProvider(javaClass<JetArrayAccessExpression>()) {
                JetArrayAccessReference(it)
            }
            registerProvider(javaClass<JetForExpression>()) {
                JetForLoopInReference(it)
            }
            registerProvider(javaClass<JetPropertyDelegate>()) {
                JetPropertyDelegationMethodsReference(it)
            }
            registerProvider(javaClass<JetMultiDeclaration>()) {
                JetMultiDeclarationReference(it)
            }
            registerProvider(javaClass<KDocName>()) {
                KDocReference(it)
            }
        }
    }

    private fun <E : JetElement, R : AbstractJetReference<E>> PsiReferenceRegistrar.registerProvider(
            elementClass: Class<E>,
            factory: (E) -> R
        ) {
        registerReferenceProvider(PlatformPatterns.psiElement(elementClass), object: PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                [suppress("UNCHECKED_CAST")]
                    return array(factory(element as E))
                }
        })
    }
}
