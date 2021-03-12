/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId

internal class KtFirMemberFunctionSymbolPointer(
    ownerClassId: ClassId,
    private val signature: IdSignature
) : KtFirMemberSymbolPointer<KtFunctionSymbol>(ownerClassId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirDeclaration>,
        firSession: FirSession
    ): KtFunctionSymbol? {
        val firFunction = candidates.findDeclarationWithSignature<FirSimpleFunction>(signature, firSession) ?: return null
        return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firFunction)
    }
}

