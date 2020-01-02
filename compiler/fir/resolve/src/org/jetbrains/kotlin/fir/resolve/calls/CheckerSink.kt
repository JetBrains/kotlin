/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import kotlin.coroutines.Continuation

abstract class CheckerSink {
    abstract fun reportApplicability(new: CandidateApplicability)

    abstract val components: InferenceComponents

    abstract val needYielding: Boolean

    @Deprecated(
        "This function yields unconditionally, exposed only for yieldIfNeed",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("yieldIfNeed")
    )
    abstract suspend fun yield()
}

suspend inline fun CheckerSink.yieldIfNeed() {
    if (needYielding) {
        @Suppress("DEPRECATION")
        yield()
    }
}

suspend inline fun CheckerSink.yieldApplicability(new: CandidateApplicability) {
    reportApplicability(new)
    yieldIfNeed()
}

class CheckerSinkImpl(override val components: InferenceComponents, var continuation: Continuation<Unit>? = null) : CheckerSink() {
    var current = CandidateApplicability.RESOLVED
    override fun reportApplicability(new: CandidateApplicability) {
        if (new < current) current = new
    }

    @Suppress("OverridingDeprecatedMember")
    override suspend fun yield() = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Unit> {
        continuation = it
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
    }

    override val needYielding: Boolean
        get() = current < CandidateApplicability.SYNTHETIC_RESOLVED

}
