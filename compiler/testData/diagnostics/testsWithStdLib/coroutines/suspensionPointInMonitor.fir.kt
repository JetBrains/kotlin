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
            suspensionPoint()
        }

        synchronized(lock) label@{
            suspensionPoint()
        }

        synchronized(lock, { suspensionPoint() })

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
            suspensionPoint()
        }
    }
}

suspend fun run() {
    synchronized(lock) {
        suspensionPoint()
    }
}

suspend fun ifWhenAndOtherNonsence() {
    synchronized(lock) {
        if (lock == Any()) {
            when (1) {
                is Int -> {
                    return@synchronized 1 + returnsInt()
                }
                else -> {}
            }
        } else {}
    }
}

suspend fun returnsInt(): Int = 0