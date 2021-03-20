/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirMemberPropertySymbolPointer(
    ownerClassId: ClassId,
    private val name: Name,
    private val signature: IdSignature
) : KtFirMemberSymbolPointer<KtKotlinPropertySymbol>(ownerClassId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignature<FirProperty>(signature, firSession) { processPropertiesByName(name, it) }
            ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firProperty) as? KtKotlinPropertySymbol
    }
}

