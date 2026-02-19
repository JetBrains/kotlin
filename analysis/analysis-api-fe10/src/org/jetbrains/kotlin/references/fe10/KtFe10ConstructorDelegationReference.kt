/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import org.jetbrains.kotlin.idea.references.KtConstructorDelegationReference
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets

@OptIn(KtImplementationDetail::class)
internal class KtFe10ConstructorDelegationReference(
    expression: KtConstructorDelegationReferenceExpression
) : KtConstructorDelegationReference(expression), KtFe10Reference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtConstructorDelegationReferenceExpression> {
        override val elementClass: Class<KtConstructorDelegationReferenceExpression>
            get() = KtConstructorDelegationReferenceExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtConstructorDelegationReferenceExpression>
            get() = { listOf(KtFe10ConstructorDelegationReference(it)) }
    }
}