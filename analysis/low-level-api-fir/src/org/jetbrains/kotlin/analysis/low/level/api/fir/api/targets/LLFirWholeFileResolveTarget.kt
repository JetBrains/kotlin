/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isDeclarationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile

/**
 * [LLFirResolveTarget] representing all declarations in file. All of them are going to be resolved.
 */
class LLFirWholeFileResolveTarget(firFile: FirFile) : LLFirResolveTarget(firFile, emptyList()) {
    override fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        fun goInside(target: FirElementWithResolveState) {
            action(target)
            if (target is FirDeclaration && target.isDeclarationContainer) {
                target.forEachDeclaration(::goInside)
            }
        }

        action(firFile)
        forEachTopLevelDeclaration(::goInside)
    }

    inline fun forEachTopLevelDeclaration(action: (FirElementWithResolveState) -> Unit) {
        firFile.annotationsContainer?.let { action(it) }

        for (member in firFile.declarations) {
            action(member)
        }
    }

    override fun toStringForTarget(): String = "*"
}
