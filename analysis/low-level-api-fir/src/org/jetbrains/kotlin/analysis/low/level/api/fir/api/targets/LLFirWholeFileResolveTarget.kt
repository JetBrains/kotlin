/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

/**
 * [LLFirResolveTarget] representing all declarations in file. All of them are going to be resolved.
 */
class LLFirWholeFileResolveTarget(
    override val firFile: FirFile,
) : LLFirResolveTarget() {
    override val path: List<FirRegularClass> get() = emptyList()

    override fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        fun goInside(target: FirElementWithResolveState) {
            action(target)
            if (target is FirRegularClass) {
                target.declarations.forEach(::goInside)
            }
        }
        forEachTopLevelDeclaration(::goInside)
    }

    inline fun forEachTopLevelDeclaration(action: (FirElementWithResolveState) -> Unit) {
        action(firFile.annotationsContainer)

        for (member in firFile.declarations) {
            action(member)
        }
    }

    override fun toStringForTarget(): String = "*"
}
