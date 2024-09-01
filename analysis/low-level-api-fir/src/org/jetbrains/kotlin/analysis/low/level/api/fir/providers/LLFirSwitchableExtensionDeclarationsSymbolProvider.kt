/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider

/**
 * An implementation of [FirSwitchableExtensionDeclarationsSymbolProvider] for Analysis API mode of the compiler.
 *
 * Instead of a bare [Boolean] for [disabled] flag, uses a backing [disabledThreadLocal] to avoid issues
 * in a multithreaded resolve environment.
 *
 * @see org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
 */
internal class LLFirSwitchableExtensionDeclarationsSymbolProvider private constructor(
    delegate: FirExtensionDeclarationsSymbolProvider
) : FirSwitchableExtensionDeclarationsSymbolProvider(delegate) {
    companion object {
        fun createIfNeeded(session: FirSession): LLFirSwitchableExtensionDeclarationsSymbolProvider? =
            FirExtensionDeclarationsSymbolProvider.createIfNeeded(session)?.let { LLFirSwitchableExtensionDeclarationsSymbolProvider(it) }
    }

    /**
     * A backing [ThreadLocal] storage for [disabled] property.
     *
     * Motivation:
     *
     * [FirSwitchableExtensionDeclarationsSymbolProvider] potentially can be accessed from multiple threads
     * during lazy resolve in the Analysis API.
     * Different threads can potentially want this provider to be either enabled or disabled,
     * depending on what they are trying to resolve.
     *
     * To allow this behavior on a per-thread basis, the backing [ThreadLocal] storage is introduced.
     *
     * N.B. We assume that there should be no thread-switching in the compiler code (e.g., due to coroutines).
     */
    private val disabledThreadLocal: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    override var disabled: Boolean
        get() = disabledThreadLocal.get()
        set(value) {
            disabledThreadLocal.set(value)
        }
}