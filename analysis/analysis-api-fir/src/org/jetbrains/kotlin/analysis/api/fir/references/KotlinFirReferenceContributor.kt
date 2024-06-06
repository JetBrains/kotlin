/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.idea.references.KotlinPsiReferenceRegistrar
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.references.ReferenceAccess

internal class KotlinFirReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KaFirForLoopInReference)
            registerProvider(factory = ::KaFirInvokeFunctionReference)
            registerProvider(factory = ::KaFirPropertyDelegationMethodsReference)
            registerProvider(factory = ::KaFirDestructuringDeclarationReference)
            registerProvider(factory = ::KaFirArrayAccessReference)
            registerProvider(factory = ::KaFirConstructorDelegationReference)
            registerProvider(factory = ::KaFirCollectionLiteralReference)
            registerProvider(factory = ::KaFirKDocReference)

            registerMultiProvider<KtSimpleNameExpression> { nameReferenceExpression ->
                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = true)) {
                    ReferenceAccess.READ -> arrayOf(KaFirSimpleNameReference(nameReferenceExpression, isRead = true))
                    ReferenceAccess.WRITE -> arrayOf(KaFirSimpleNameReference(nameReferenceExpression, isRead = false))
                    ReferenceAccess.READ_WRITE -> arrayOf(
                        KaFirSimpleNameReference(nameReferenceExpression, isRead = true),
                        KaFirSimpleNameReference(nameReferenceExpression, isRead = false),
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
