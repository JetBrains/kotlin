/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass


sealed class LLFirDesignationToResolve {
    abstract val firFile: FirFile
    abstract val path: List<FirRegularClass>
}

class LLFirDesignationForResolveWithMembers(
    override val firFile: FirFile,
    override val path: List<FirRegularClass>,
    val target: FirRegularClass,
    val callableMembersToResolve: List<FirDeclaration>
) : LLFirDesignationToResolve() {
    fun asDesignationForTarget(): FirDesignationWithFile =
        FirDesignationWithFile(path, target, firFile)

}

class LLFirDesignationForResolveWithMultipleTargets(
    override val firFile: FirFile,
    override val path: List<FirRegularClass>,
    val targets: List<FirElementWithResolveState>,
    val resolveMembersInsideTarget: Boolean,
) : LLFirDesignationToResolve() {
    init {
        require(targets.isNotEmpty()) {
            "Resolve targets cannot be empty"
        }
    }

    fun asDesignationSequence(): Sequence<FirDesignationWithFile> = sequence {
        for (target in targets) {
            yield(FirDesignationWithFile(path, target, firFile))
        }
    }
}

fun FirDesignationWithFile.asMultiDesignation(resolveMembersInsideTarget: Boolean): LLFirDesignationToResolve =
    LLFirDesignationForResolveWithMultipleTargets(
        firFile,
        path,
        listOf(target),
        resolveMembersInsideTarget,
    )