// TARGET_BACKEND: JVM
// JVM_TARGET: 11
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*
import kotlin.concurrent.thread

class Counters {
    @Volatile var ints: Int = 0
    @Volatile var longs: Long = 0L
    @Volatile var updates: Int = 0
    @Volatile var list: List<Int> = emptyList()
    @Volatile var flag: Boolean = false
    @Volatile var flagWinners: Int = 0

    fun work(id: Int, iterations: Int) {
        repeat(iterations) {
            this::ints.incrementAndFetch()
            this::longs += 2L
            this::updates.update { it + 3 }
        }
        this::list.update { it + id }
        if (this::flag.compareAndSet(false, true)) this::flagWinners.incrementAndFetch()
    }
}

fun box(): String {
    val threads = 8
    val iterations = 20_000
    val counters = Counters()
    (0 until threads).map { id ->
        thread { counters.work(id, iterations) }
    }.forEach { it.join() }

    if (counters.ints != threads * iterations) return "Fail ints: ${counters.ints}"
    if (counters.longs != 2L * threads * iterations) return "Fail longs: ${counters.longs}"
    if (counters.updates != 3 * threads * iterations) return "Fail updates: ${counters.updates}"
    if (counters.list.sorted() != (0 until threads).toList()) return "Fail list: ${counters.list}"
    if (counters.flagWinners != 1) return "Fail flag: ${counters.flagWinners}"
    return "OK"
}
