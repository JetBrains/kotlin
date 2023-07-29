/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*

/**
 * [LLFirResolveTarget] representing single target to resolve. The [target] can be any of [FirElementWithResolveState]
 */
class LLFirSingleResolveTarget(
    firFile: FirFile,
    classPath: List<FirRegularClass>,
    target: FirElementWithResolveState,
) : LLFirResolveTargetWithDedicatedElement<FirElementWithResolveState>(firFile, classPath, target) {
    override fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        action(target)
    }

    override fun toStringForTarget(): String = when (target) {
        is FirConstructor -> "constructor"
        is FirClassLikeDeclaration -> target.symbol.name.asString()
        is FirCallableDeclaration -> target.symbol.name.asString()
        is FirAnonymousInitializer -> ("<init-block>")
        is FirFileAnnotationsContainer -> "<file annotations>"
        is FirScript -> target.name.asString()
        else -> "???"
    }
}