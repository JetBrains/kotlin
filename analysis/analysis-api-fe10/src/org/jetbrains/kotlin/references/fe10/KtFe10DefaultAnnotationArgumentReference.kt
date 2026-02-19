/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

@OptIn(KtImplementationDetail::class)
internal class KtFe10DefaultAnnotationArgumentReference(
    element: KtValueArgument,
) : KtDefaultAnnotationArgumentReference(element), KtFe10Reference {

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val annotationEntry = element.getStrictParentOfType<KtAnnotationEntry>() ?: return emptyList()
        val resolvedCall = annotationEntry.getResolvedCall(context) ?: return emptyList()
        return listOfNotNull(resolvedCall.resultingDescriptor.valueParameters.firstOrNull())
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }

    class Provider : KotlinPsiReferenceProviderContributor<KtValueArgument> {
        override val elementClass: Class<KtValueArgument>
            get() = KtValueArgument::class.java

        override val referenceProvider: KotlinPsiReferenceProviderContributor.ReferenceProvider<KtValueArgument>
            get() = { element ->
                if (element.shouldProduceReference()) {
                    listOf(KtFe10DefaultAnnotationArgumentReference(element))
                } else {
                    emptyList()
                }
            }
    }
}
