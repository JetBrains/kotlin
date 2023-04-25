/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase

internal object LLFirResolvePhaseChecker {
    fun requireResolvePhase(firDeclaration: FirDeclaration, requiredPhase: FirResolvePhase) {
        require(firDeclaration.resolvePhase == FirResolvePhase.BODY_RESOLVE) {
            "Required $requiredPhase for $firDeclaration but ${firDeclaration.resolvePhase} found"
        }
    }
}