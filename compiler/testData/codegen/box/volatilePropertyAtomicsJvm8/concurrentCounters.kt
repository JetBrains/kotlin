// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*
import kotlin.concurrent.thread

class Counters {
    @Volatile var ints: Int = 0
    @Volatile var longs: Long = 0L
    @Volatile var updates: Int = 0
    @Volatile var exchanges: Long = 0L
    @Volatile var list: List<Int> = emptyList()
    @Volatile var winner: String? = null

    fun work(id: Int, iterations: Int) {
        repeat(iterations) {
            this::ints.incrementAndFetch()
            this::longs += 2L
            this::updates.update { it + 3 }
            // An increment built on top of the emulated compareAndExchange.
            var current = exchanges
            while (true) {
                val witness = this::exchanges.compareAndExchange(current, current + 1)
                if (witness == current) break
                current = witness
            }
        }
        this::list.update { it + id }
        this::winner.compareAndExchange(null, "thread $id")
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
    if (counters.exchanges != threads.toLong() * iterations) return "Fail exchanges: ${counters.exchanges}"
    if (counters.list.sorted() != (0 until threads).toList()) return "Fail list: ${counters.list}"
    if (counters.winner == null) return "Fail winner"
    return "OK"
}
