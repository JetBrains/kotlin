/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.inspections

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

class AddFunctionReturnTypeIntention :
    AbstractHighLevelApiBasedIntention<KtNamedFunction, TypeCandidate>(
        KtNamedFunction::class.java,
        { "Specify type explicitly" }
    ) {
    override fun isApplicableByPsi(element: KtNamedFunction): Boolean =
        element.typeReference == null && !element.hasBlockBody()

    override fun KtAnalysisSession.analyzeAndGetData(element: KtNamedFunction): TypeCandidate? {
        val returnType = element.getReturnKtType()
        val approximated = approximateTypeToUpperDenotable(returnType) ?: return null
        return TypeCandidate(approximated.render())
    }

    private tailrec fun approximateTypeToUpperDenotable(type: KtType): KtDenotableType? = when (type) {
        is KtNonDenotableType -> when (type) {
            is KtFlexibleType -> approximateTypeToUpperDenotable(type.upperBound)
            is KtIntersectionType -> null
        }
        is KtDenotableType -> type
        else -> null
    }


    override fun applyTo(element: KtNamedFunction, data: TypeCandidate, editor: Editor?) {
        element.typeReference = KtPsiFactory(element).createType(data.candidate)
    }
}

data class TypeCandidate(val candidate: String)