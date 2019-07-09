/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents

internal class KotlinReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtSimpleNameReference)

            registerMultiProvider<KtNameReferenceExpression> { nameReferenceExpression ->
                if (nameReferenceExpression.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMultiProvider emptyArray()
                if (nameReferenceExpression.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@registerMultiProvider emptyArray()
                }

                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(SyntheticPropertyAccessorReference.Getter(nameReferenceExpression))
                    ReferenceAccess.WRITE ->
                        arrayOf(SyntheticPropertyAccessorReference.Setter(nameReferenceExpression))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf(
                            SyntheticPropertyAccessorReference.Getter(nameReferenceExpression),
                            SyntheticPropertyAccessorReference.Setter(nameReferenceExpression)
                        )
                }
            }

            registerProvider(factory = ::KtConstructorDelegationReference)

            registerProvider(factory = ::KtInvokeFunctionReference)

            registerProvider(factory = ::KtArrayAccessReference)

            registerProvider(factory = ::KtCollectionLiteralReference)

            registerProvider(factory = ::KtForLoopInReference)

            registerProvider(factory = ::KtPropertyDelegationMethodsReference)

            registerProvider(factory = ::KtDestructuringDeclarationReference)

            registerProvider(factory = ::KDocReference)
        }
    }
}
