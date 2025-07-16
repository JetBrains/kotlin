/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.services

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLMismatchedPsiFirElementByPsiElementChooser
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*

/**
 * In Standalone mode, deserialized elements don't have sources, so we need to implement [LLFirElementByPsiElementChooser] based on
 * component comparison (see [LLFirElementByPsiElementChooser]).
 *
 * TODO: We might be able to remove this service if KT-65836 is viable (using stub-based deserialized symbol providers in Standalone mode).
 */
class LLStandaloneFirElementByPsiElementChooser : LLMismatchedPsiFirElementByPsiElementChooser() {
    override fun isMatchingValueParameter(psi: KtParameter, fir: FirValueParameter): Boolean {
        // In contrast to `LLMismatchedPsiFirElementByPsiElementChooser`, we don't allow FIR elements to be chosen with mismatched PSI.
        if (fir.realPsi != null) return fir.realPsi === psi

        return super.isMatchingValueParameter(psi, fir)
    }

    override fun isMatchingTypeParameter(psi: KtTypeParameter, fir: FirTypeParameter): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return super.isMatchingTypeParameter(psi, fir)
    }

    override fun isMatchingEnumEntry(psi: KtEnumEntry, fir: FirEnumEntry): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return super.isMatchingEnumEntry(psi, fir)
    }

    override fun isMatchingClassLikeDeclaration(classId: ClassId, psi: KtClassLikeDeclaration, fir: FirClassLikeDeclaration): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return super.isMatchingClassLikeDeclaration(classId, psi, fir)
    }

    // TODO: Use structural type comparison? We can potentially ignore components which don't factor into overload resolution, such as type
    //       annotations, because we only need to pick one FIR callable without a reasonable doubt and ambiguities cannot originate from
    //       libraries.
    override fun isMatchingCallableDeclaration(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return super.isMatchingCallableDeclaration(psi, fir)
    }
}
