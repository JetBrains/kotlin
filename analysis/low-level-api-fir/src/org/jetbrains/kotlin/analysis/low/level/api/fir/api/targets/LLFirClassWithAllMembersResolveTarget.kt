/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

/**
 * [LLFirResolveTarget] representing a class with all class members to be resolved (this includes callables, init blocks, and classifiers)
 */
class LLFirClassWithAllMembersResolveTarget(
    override val firFile: FirFile,
    override val path: List<FirRegularClass>,
    val target: FirRegularClass,
) : LLFirResolveTarget() {
    override fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        action(target)
        forEachMember(action)
    }

    inline fun forEachMember(action: (FirDeclaration) -> Unit) {
        target.declarations.forEach(action)
    }

    override fun toStringForTarget(): String = target.name.asString()
}