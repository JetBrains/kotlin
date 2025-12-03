/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.KtCollectionLiteralReference
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext

@OptIn(KtImplementationDetail::class)
internal class KtFe10CollectionLiteralReference(
    expression: KtCollectionLiteralExpression
) : KtCollectionLiteralReference(expression), KtFe10Reference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val resolvedCall = context[BindingContext.COLLECTION_LITERAL_CALL, element]
        return listOfNotNull(resolvedCall?.resultingDescriptor)
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtCollectionLiteralExpression> {
        override val elementClass: Class<KtCollectionLiteralExpression>
            get() = KtCollectionLiteralExpression::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtCollectionLiteralExpression>
            get() = { listOf(KtFe10CollectionLiteralReference(it)) }
    }
}
