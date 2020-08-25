/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contract.contextual.diagnostics.CoeffectContextVerificationError

interface CoeffectContextAction {
    val family: CoeffectFamily
}

interface CoeffectContextCleaner : CoeffectContextAction {
    fun cleanupContext(context: CoeffectContext): CoeffectContext
}

interface CoeffectContextProvider : CoeffectContextAction {
    fun provideContext(context: CoeffectContext): CoeffectContext
}

interface CoeffectContextVerifier : CoeffectContextAction {
    fun verifyOnCurrentNode(): Boolean = false
    fun verifyContext(context: CoeffectContext, session: FirSession): List<CoeffectContextVerificationError>
}

class CoeffectContextActions(
    val provider: CoeffectContextProvider? = null,
    val verifier: CoeffectContextVerifier? = null,
    val cleaner: CoeffectContextCleaner? = null
) {

    companion object {
        val EMPTY = CoeffectContextActions()
    }

    val isEmpty: Boolean get() = provider == null && verifier == null && cleaner == null

}