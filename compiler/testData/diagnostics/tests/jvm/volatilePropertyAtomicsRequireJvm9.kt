// RUN_PIPELINE_TILL: LOWERINGS
// WITH_STDLIB
// JVM_TARGET: 1.8
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.atomics.*

class Counter {
    @Volatile var x: Int = 0

    fun inc(): Int = <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::x.incrementAndFetch()<!>
}
