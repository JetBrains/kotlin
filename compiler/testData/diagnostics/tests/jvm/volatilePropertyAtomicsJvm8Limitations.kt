// RUN_PIPELINE_TILL: LOWERINGS
// WITH_STDLIB
// JVM_TARGET: 1.8
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

// On JVM 1.8, atomic operations are implemented with `Atomic*FieldUpdater`s, which do not support `boolean` fields.
// (Static fields are not supported by field updaters either, but they are prohibited by the frontend on all platforms.)

import kotlin.concurrent.atomics.*

class Supported {
    @Volatile var i: Int = 0
    @Volatile var l: Long = 0L
    @Volatile var s: String = ""
    @Volatile var b: Boolean = false

    fun test() {
        this::i.incrementAndFetch()
        this::i.compareAndExchange(1, 2)
        this::l.update { it + 1 }
        this::s.compareAndSet("", "a")
        this::s.compareAndExchange("a", "b")
    }

    fun unsupportedBoolean() {
        <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::b.compareAndSet(false, true)<!>
        <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::b.exchange(true)<!>
    }
}
