/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * Returns the restored [KaType] (possibly a new type instance) if the pointer is still valid, or `null` otherwise.
 */
@KaExperimentalApi
context(session: KaSession)
public fun <T : KaType> KaTypePointer<T>.restore(): T? = session.withValidityAssertion {
    @OptIn(KaImplementationDetail::class)
    restore(session)
}
