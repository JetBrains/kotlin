/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.util.PrivateForInline
import kotlin.coroutines.Continuation

abstract class CheckerSink {
    abstract fun reportDiagnostic(diagnostic: ResolutionDiagnostic)

    abstract val needYielding: Boolean

    @PrivateForInline
    abstract suspend fun yield()
}

@OptIn(PrivateForInline::class)
suspend inline fun CheckerSink.yieldIfNeed() {
    if (needYielding) {
        yield()
    }
}

suspend inline fun CheckerSink.yieldDiagnostic(diagnostic: ResolutionDiagnostic) {
    reportDiagnostic(diagnostic)
    yieldIfNeed()
}

class CheckerSinkImpl(
    private val candidate: Candidate,
    var continuation: Continuation<Unit>? = null,
    val stopOnFirstError: Boolean = true,
) : CheckerSink() {
    override fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    @PrivateForInline
    override suspend fun yield() = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Unit> {
        continuation = it
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
    }

    override val needYielding: Boolean
        get() = stopOnFirstError && !candidate.isSuccessful
}

