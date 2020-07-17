/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import kotlin.coroutines.Continuation

abstract class CheckerSink {
    abstract fun reportApplicability(new: CandidateApplicability)

    abstract val components: InferenceComponents

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

suspend inline fun CheckerSink.yieldApplicability(new: CandidateApplicability) {
    reportApplicability(new)
    yieldIfNeed()
}

class CheckerSinkImpl(
    override val components: InferenceComponents,
    var continuation: Continuation<Unit>? = null,
    val stopOnFirstError: Boolean = true
) : CheckerSink() {
    var current = CandidateApplicability.RESOLVED
        private set

    override fun reportApplicability(new: CandidateApplicability) {
        if (new < current) current = new
    }

    @PrivateForInline
    override suspend fun yield() = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Unit> {
        continuation = it
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
    }

    override val needYielding: Boolean
        get() = stopOnFirstError && current < CandidateApplicability.SYNTHETIC_RESOLVED

}
