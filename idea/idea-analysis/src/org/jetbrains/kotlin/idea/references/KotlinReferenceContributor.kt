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

import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinReferenceContributor() : AbstractKotlinReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtSimpleNameReference)

            registerMultiProvider<KtNameReferenceExpression> {
                if (it.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMultiProvider emptyArray()
                if (it.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@registerMultiProvider emptyArray()
                }

                when (it.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Getter(it))
                    ReferenceAccess.WRITE ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Setter(it))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf<PsiReference>(SyntheticPropertyAccessorReference.Getter(it), SyntheticPropertyAccessorReference.Setter(it))
                }
            }

            registerProvider(factory = ::KtConstructorDelegationReference)

            registerProvider(factory = ::KtInvokeFunctionReference)

            registerProvider(factory = ::KtArrayAccessReference)

            registerProvider(factory = ::KtForLoopInReference)

            registerProvider(factory = ::KtPropertyDelegationMethodsReference)

            registerProvider(factory = ::KtDestructuringDeclarationReference)

            registerProvider(factory = ::KDocReference)
        }
    }
}
