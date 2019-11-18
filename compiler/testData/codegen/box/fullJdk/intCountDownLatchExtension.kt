// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FULL_JDK

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock

fun <T> Int.latch(op: CountDownLatch.() -> T) : T {
    val cdl = CountDownLatch(this)
    val res = cdl.op()
    cdl.await()
    return res
}

fun id(op: () -> Unit) = op()

fun box() : String {
    1.latch{
        id {
            countDown()
        }
    }
    return "OK"
}
