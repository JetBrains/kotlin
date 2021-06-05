/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.frontend.api.components.KtPsiTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.getReferencedElementType
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference

internal class KtFirPsiTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtPsiTypeProvider(), KtFirAnalysisSessionComponent {
    override fun getPsiTypeForKtExpression(
        expression: KtExpression,
        mode: TypeMappingMode,
    ): PsiType = withValidityAssertion {
        when (val fir = expression.getOrBuildFir(firResolveState)) {
            is FirExpression -> fir.typeRef.coneType.asPsiType(mode, expression)
            is FirNamedReference -> fir.getReferencedElementType().asPsiType(mode, expression)
            is FirStatement -> PsiType.VOID
            else -> error("Unexpected ${fir::class}")
        }
    }

    override fun getPsiTypeForKtTypeReference(
        ktTypeReference: KtTypeReference,
        mode: TypeMappingMode
    ): PsiType = withValidityAssertion {
        when (val fir = ktTypeReference.getOrBuildFir(firResolveState)) {
            // NB: [FirErrorTypeRef] is a subtype of [FirResolvedTypeRef], and the error type in it will be properly handled by [asPsiType].
            is FirResolvedTypeRef -> fir.coneType.asPsiType(mode, ktTypeReference)
            else -> error("Unexpected ${fir::class}")
        }
    }
}
