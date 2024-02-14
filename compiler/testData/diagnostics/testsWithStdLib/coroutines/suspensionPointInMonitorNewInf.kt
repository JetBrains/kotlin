// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
// !DIAGNOSTICS: -UNUSED_PARAMETER, -USELESS_IS_CHECK
// SKIP_TXT

import kotlin.concurrent.withLock

val lock = java.util.concurrent.locks.ReentrantLock()

fun builder(c: suspend () -> Unit) {}

suspend fun getLock() = lock

suspend fun suspensionPoint() {}

fun test() {
    builder {
        synchronized(lock) {
            <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>suspensionPoint<!>()
        }

        synchronized(lock) label@{
            <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>suspensionPoint<!>()
        }

        synchronized(lock, { <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>suspensionPoint<!>() })

        synchronized(getLock()) {
            println("")
        }
        synchronized(suspend { getLock() } ()) {
            println("")
        }
        synchronized(run { getLock() }) {
            println("")
        }
        lock.withLock {
            <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>suspensionPoint<!>()
        }
    }
}

suspend fun run() {
    synchronized(lock) {
        <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>suspensionPoint<!>()
    }
}

suspend fun ifWhenAndOtherNonsence() {
    synchronized(lock) {
        if (lock == Any()) {
            when (1) {
                is Int -> {
                    return@synchronized 1 + <!SUSPENSION_POINT_INSIDE_CRITICAL_SECTION!>returnsInt<!>()
                }
                else -> {}
            }
        } else {}
    }
}

suspend fun returnsInt(): Int = 0
