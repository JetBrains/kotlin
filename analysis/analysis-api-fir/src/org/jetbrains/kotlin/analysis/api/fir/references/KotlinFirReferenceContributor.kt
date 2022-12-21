/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.analysis.api.fir.references.KtFirKDocReference
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.references.ReferenceAccess

class KotlinFirReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtFirForLoopInReference)
            registerProvider(factory = ::KtFirInvokeFunctionReference)
            registerProvider(factory = ::KtFirPropertyDelegationMethodsReference)
            registerProvider(factory = ::KtFirDestructuringDeclarationReference)
            registerProvider(factory = ::KtFirArrayAccessReference)
            registerProvider(factory = ::KtFirConstructorDelegationReference)
            registerProvider(factory = ::KtFirCollectionLiteralReference)
            registerProvider(factory = ::KtFirKDocReference)

            registerMultiProvider<KtSimpleNameExpression> { nameReferenceExpression ->
                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = true)) {
                    ReferenceAccess.READ -> arrayOf(KtFirSimpleNameReference(nameReferenceExpression, isRead = true))
                    ReferenceAccess.WRITE -> arrayOf(KtFirSimpleNameReference(nameReferenceExpression, isRead = false))
                    ReferenceAccess.READ_WRITE -> arrayOf(
                        KtFirSimpleNameReference(nameReferenceExpression, isRead = true),
                        KtFirSimpleNameReference(nameReferenceExpression, isRead = false),
                    )
                }
            }

            registerProvider provider@{ element: KtValueArgument ->
                if (element.isNamed()) return@provider null
                val annotationEntry = element.getParentOfTypeAndBranch<KtAnnotationEntry> { valueArgumentList } ?: return@provider null
                if (annotationEntry.valueArguments.size != 1) return@provider null

                KtDefaultAnnotationArgumentReference(element)
            }
        }
    }
}
