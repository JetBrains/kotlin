/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.contextual

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.contextual.diagnostics.CoeffectContextVerificationError

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
    val needVerifyOnCurrentNode: Boolean get() = false
    fun verifyContext(context: CoeffectContext, session: FirSession): List<CoeffectContextVerificationError>
}

class CoeffectContextActions(
    val providers: List<CoeffectContextProvider> = emptyList(),
    val verifiers: List<CoeffectContextVerifier> = emptyList(),
    val cleaners: List<CoeffectContextCleaner> = emptyList()
) {

    companion object {
        val EMPTY = CoeffectContextActions()
    }

    val isEmpty: Boolean get() = providers.isEmpty() && verifiers.isEmpty() && cleaners.isEmpty()

}

class CoeffectContextActionsBuilder {
    val providers = mutableListOf<CoeffectContextProvider>()
    val verifiers = mutableListOf<CoeffectContextVerifier>()
    val cleaners = mutableListOf<CoeffectContextCleaner>()

    fun build(): CoeffectContextActions = CoeffectContextActions(providers, verifiers, cleaners)
}

inline fun coeffectActions(block: CoeffectContextActionsBuilder.() -> Unit): CoeffectContextActions {
    val builder = CoeffectContextActionsBuilder()
    block(builder)
    return builder.build()
}