/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaSession

interface KaSessionCreationContext<T : KaSession> {
    val analysisSessionProvider: () -> T
}

private class KaSessionCreationContextImpl<T : KaSession> : () -> T, KaSessionCreationContext<T> {
    private var cachedSession: T? = null

    fun initialize(session: T) {
        require(cachedSession == null) { "The session is already initialized" }
        cachedSession = session
    }

    override val analysisSessionProvider: () -> T
        get() = this

    override fun invoke(): T {
        return cachedSession
            ?: error(
                "Session is not yet initialized. " +
                        "If you are inside a session component, perhaps you will need to wrap your computation in 'lazy {}'"
            )
    }
}

fun <T : KaSession> createSession(block: KaSessionCreationContext<T>.() -> T): T {
    val box = KaSessionCreationContextImpl<T>()
    val session = block(box)
    box.initialize(session)
    return session
}