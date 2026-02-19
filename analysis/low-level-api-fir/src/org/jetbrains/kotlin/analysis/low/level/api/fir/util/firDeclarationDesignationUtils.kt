/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

internal fun FirElementWithResolveState.checkPhase(requiredResolvePhase: FirResolvePhase) {
    @OptIn(ResolveStateAccess::class)
    val declarationResolveState = resolveState
    checkWithAttachment(
        declarationResolveState.resolvePhase >= requiredResolvePhase,
        { "At least $requiredResolvePhase expected but $declarationResolveState found for ${this::class.simpleName}" },
    ) {
        withFirEntry("firDeclaration", this@checkPhase)
    }
}
