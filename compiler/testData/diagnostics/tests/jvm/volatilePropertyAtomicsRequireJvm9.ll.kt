// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: LOWERINGS
// WITH_STDLIB
// JVM_TARGET: 1.8
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.atomics.*

class Counter {
    @Volatile var x: Int = 0

    fun inc(): Int = this::x.incrementAndFetch()
}
