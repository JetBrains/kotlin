/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.capture

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

/**
 * A [KotlinLogger] decorator that forwards every message to [delegate] (preserving normal build-log output)
 * and additionally records it to [BtaEventCapture], tagged with the per-module [context].
 *
 * It reports [isDebugEnabled] as `true` so that the compiler produces its full incremental-compilation
 * detail: in-process the debug branches of the IC reporter fire, and in daemon mode the client asks the
 * daemon to stream back the verbose build-report lines (which carry the "dirty member/class" reasons).
 * The [delegate] still applies its own gating for what actually reaches the console.
 *
 * Installed only when [BtaEventCapture.isEnabled] is `true`, so it has no effect on normal builds.
 */
internal class CapturingKotlinLogger(
    private val delegate: KotlinLogger,
    private val context: EventContext,
) : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = true

    override fun error(msg: String, throwable: Throwable?) {
        BtaEventCapture.record(context, "ERROR", msg)
        delegate.error(msg, throwable)
    }

    override fun warn(msg: String, throwable: Throwable?) {
        BtaEventCapture.record(context, "WARN", msg)
        delegate.warn(msg, throwable)
    }

    override fun info(msg: String) {
        BtaEventCapture.record(context, "INFO", msg)
        delegate.info(msg)
    }

    override fun debug(msg: String) {
        BtaEventCapture.record(context, "DEBUG", msg)
        delegate.debug(msg)
    }

    override fun lifecycle(msg: String) {
        BtaEventCapture.record(context, "LIFECYCLE", msg)
        delegate.lifecycle(msg)
    }
}
