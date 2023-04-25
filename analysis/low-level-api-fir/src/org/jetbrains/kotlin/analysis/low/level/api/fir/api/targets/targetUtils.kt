/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.fir.FirElementWithResolveState

fun LLFirResolveTarget.forEachPathElementAndTarget(action: (FirElementWithResolveState) -> Unit) {
    path.forEach(action)
    forEachTarget(action)
}

fun FirDesignationWithFile.asResolveTarget(): LLFirSingleResolveTarget = LLFirSingleResolveTarget(firFile, path, target)