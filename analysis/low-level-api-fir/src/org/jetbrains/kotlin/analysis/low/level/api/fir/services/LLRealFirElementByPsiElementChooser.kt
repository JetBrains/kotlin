/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter

internal class LLRealFirElementByPsiElementChooser : LLFirElementByPsiElementChooser() {
    override fun isMatchingValueParameter(psi: KtParameter, fir: FirValueParameter): Boolean = fir.realPsi === psi

    override fun isMatchingTypeParameter(psi: KtTypeParameter, fir: FirTypeParameter): Boolean = fir.realPsi === psi

    override fun isMatchingEnumEntry(psi: KtEnumEntry, fir: FirEnumEntry): Boolean = fir.realPsi === psi

    override fun isMatchingCallableDeclaration(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean = fir.realPsi === psi
}
