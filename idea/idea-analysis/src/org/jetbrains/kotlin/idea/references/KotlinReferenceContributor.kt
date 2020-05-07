/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtSimpleNameReferenceDescriptorsImpl)

            registerMultiProvider<KtNameReferenceExpression> { nameReferenceExpression ->
                if (nameReferenceExpression.getReferencedNameElementType() != KtTokens.IDENTIFIER) return@registerMultiProvider emptyArray()
                if (nameReferenceExpression.parents.any { it is KtImportDirective || it is KtPackageDirective || it is KtUserType }) {
                    return@registerMultiProvider emptyArray()
                }

                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = true))
                    ReferenceAccess.WRITE ->
                        arrayOf(SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = false))
                    ReferenceAccess.READ_WRITE ->
                        arrayOf(
                            SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = true),
                            SyntheticPropertyAccessorReferenceDescriptorImpl(nameReferenceExpression, getter = false)
                        )
                }
            }

            registerProvider(factory = ::KtConstructorDelegationReferenceDescriptorsImpl)

            registerProvider(factory = ::KtInvokeFunctionReferenceDescriptorsImpl)

            registerProvider(factory = ::KtArrayAccessReferenceDescriptorsImpl)

            registerProvider(factory = ::KtCollectionLiteralReferenceDescriptorsImpl)

            registerProvider(factory = ::KtForLoopInReferenceDescriptorsImpl)

            registerProvider(factory = ::KtPropertyDelegationMethodsReferenceDescriptorsImpl)

            registerProvider(factory = ::KtDestructuringDeclarationReferenceDescriptorsImpl)

            registerProvider(factory = ::KDocReference)

            registerProvider(KotlinDefaultAnnotationMethodImplicitReferenceProvider)
        }
    }
}
