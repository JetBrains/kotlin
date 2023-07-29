/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

/**
 * [LLFirResolveTarget] representing all declarations in a class (including nested classes).
 * All of them are going to be resolved.
 */
class LLFirWholeClassResolveTarget(
    firFile: FirFile,
    classPath: List<FirRegularClass>,
    target: FirRegularClass,
) : LLFirResolveTargetWithDedicatedElement<FirRegularClass>(firFile, classPath, target) {
    override fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        action(target)

        fun goInside(target: FirElementWithResolveState) {
            action(target)
            if (target is FirRegularClass) {
                target.declarations.forEach(::goInside)
            }
        }

        forEachDeclaration(::goInside)
    }

    inline fun forEachDeclaration(action: (FirElementWithResolveState) -> Unit) {
        for (member in target.declarations) {
            action(member)
        }
    }

    override fun toStringForTarget(): String = "*"
}
