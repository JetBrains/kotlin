/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import kotlin.coroutines.Continuation

interface CheckerSink {
    fun reportApplicability(new: CandidateApplicability)
    suspend fun yield()
    suspend fun yieldApplicability(new: CandidateApplicability) {
        reportApplicability(new)
        yield()
    }

    val components: InferenceComponents

    suspend fun yieldIfNeed()
}

class CheckerSinkImpl(override val components: InferenceComponents, var continuation: Continuation<Unit>? = null) : CheckerSink {
    var current = CandidateApplicability.RESOLVED
    override fun reportApplicability(new: CandidateApplicability) {
        if (new < current) current = new
    }

    override suspend fun yield() = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Unit> {
        continuation = it
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
    }

    override suspend fun yieldIfNeed() {
        if (current < CandidateApplicability.SYNTHETIC_RESOLVED) {
            yield()
        }
    }
}
