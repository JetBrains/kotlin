// RUN_PIPELINE_TILL: LOWERINGS
// WITH_STDLIB
// JVM_TARGET: 1.8
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

// On JVM 1.8, atomic operations are implemented with `Atomic*FieldUpdater`s, which support neither static nor `boolean` fields.

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
        // load/store are plain volatile accesses.
        this::b.store(this::b.load())
    }

    fun unsupportedBoolean() {
        <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::b.compareAndSet(false, true)<!>
        <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::b.exchange(true)<!>
    }

    companion object {
        @Volatile var c: Int = 0

        fun unsupportedCompanion() {
            this::c.store(this::c.load() + 1)
            <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::c.incrementAndFetch()<!>
        }
    }
}

object Obj {
    @Volatile var o: Long = 0L

    fun unsupportedObject() {
        <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>this::o.fetchAndAdd(1L)<!>
    }
}

@Volatile var topLevel: String = ""

fun unsupportedTopLevel() {
    ::topLevel.store("a")
    <!VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9!>::topLevel.update { it + "b" }<!>
}
