namespace lockperformance

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

fun thread(f: fun ()) {
    val thread = Thread(
        object: Runnable {
            override fun run() {
                f()
            }
        }
    )
    thread.start()
}

fun <T> Int.latch(op: fun CountDownLatch.() : T) : T {
    val cdl = CountDownLatch(this)
    val res = cdl.op()
    cdl.await()
    return res
}

fun Int.times(action: fun Int.()) {
    for(i in 0..this-1)
        action()
}

fun main(args: Array<String>) {
    val processors = Runtime.getRuntime().sure().availableProcessors()
    var threadNum = 1
    while(threadNum <= 1024) {
        val counter = AtomicInteger()

        val start = System.currentTimeMillis()
        threadNum.latch{
            val lock = ReentrantLock()
            threadNum.times {
                thread {
                    while(true) {
                        lock.lock()
                        try {
                            if (counter.get() == 100000000) {
                                countDown();
                                break;
                            } else {
                                counter.incrementAndGet();
                            }
                        }
                        finally {
                            lock.unlock()
                        }
                    }
                }
            }
        }

        System.out?.println(threadNum.toString() + " " + (System.currentTimeMillis() - start));

        if(threadNum < 2 * processors)
            threadNum = threadNum + 1
        else
            threadNum = threadNum * 2
    }
}
