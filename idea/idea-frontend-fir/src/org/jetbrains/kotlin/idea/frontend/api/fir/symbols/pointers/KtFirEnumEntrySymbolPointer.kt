/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId

internal class KtFirEnumEntrySymbolPointer(
    ownerClassId: ClassId,
    private val signature: IdSignature
) : KtFirMemberSymbolPointer<KtEnumEntrySymbol>(ownerClassId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirDeclaration>,
        firSession: FirSession
    ): KtEnumEntrySymbol? {
        val firProperty = candidates.findDeclarationWithSignature<FirEnumEntry>(signature, firSession) ?: return null
        return firSymbolBuilder.buildEnumEntrySymbol(firProperty)
    }
}

