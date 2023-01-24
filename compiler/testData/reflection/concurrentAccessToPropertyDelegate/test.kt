import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread
import kotlin.reflect.jvm.*

const val N_THREADS = 50

class Delegate {
    operator fun getValue(x: Any?, y: Any?): String = "OK"
    operator fun setValue(x: Any?, y: Any?, z: String) {}
}

var property by Delegate()

fun main() {
    val reference = ::property

    val gate = CyclicBarrier(N_THREADS + 1)
    var fail = AtomicReference<Throwable?>(null)
    var finished = AtomicInteger(0)
    for (i in 0 until N_THREADS) {
        thread {
            gate.await()
            reference.isAccessible = true
            try {
                reference.getDelegate()!!
            } catch (e: Throwable) {
                fail.set(e)
            }
            finished.incrementAndGet()
        }
    }

    gate.await()

    while (finished.get() != N_THREADS) Thread.sleep(25L)
    fail.get()?.let { throw it }
}
