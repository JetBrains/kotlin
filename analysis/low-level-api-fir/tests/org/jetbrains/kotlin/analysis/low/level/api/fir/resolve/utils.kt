/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.Assertions

/**
 * Asserts the consistency of the [FirFile] for a given [KtFile] file
 * before and after executing the provided action. This method ensures that no modifications are made
 * to the FIR tree during the provided action, which could lead to invalid or unexpected behavior.
 *
 * @param T the return type of the action.
 * @param file the file to be checked for FIR tree consistency.
 * @param action a lambda function representing the action to be executed while ensuring FIR tree consistency.
 * @return the result of the action execution.
 */
internal fun <T> Assertions.assertFirTreeConsistency(file: KtFile, action: () -> T): T = withResolutionFacade(file) { resolutionFacade ->
    val firFile = file.getOrBuildFirFile(resolutionFacade)
    firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
    lazyResolveRenderer(StringBuilder())
    val dumpBefore = firFile.dumpOutput()
    val output = action()
    val dumpAfter = firFile.dumpOutput()
    assertEquals(dumpBefore, dumpAfter) {
        "Fir tree has been changed during the resolve which is illegal"
    }

    output
}

private fun FirFile.dumpOutput(): String {
    return lazyResolveRenderer(StringBuilder()).renderElementAsString(this)
}
