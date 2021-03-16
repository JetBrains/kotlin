/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId

internal class KtFirMemberPropertySymbolPointer(
    ownerClassId: ClassId,
    private val signature: IdSignature
) : KtFirMemberSymbolPointer<KtKotlinPropertySymbol>(ownerClassId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirDeclaration>,
        firSession: FirSession
    ): KtKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignature<FirProperty>(signature, firSession) ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firProperty) as? KtKotlinPropertySymbol
    }
}

