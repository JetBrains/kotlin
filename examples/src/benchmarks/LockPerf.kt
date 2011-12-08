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

fun main(args: Array<String>) {
    val processors = Runtime.getRuntime().sure().availableProcessors()
    var threadNum = 1
    while(threadNum <= 1024) {
        val counter = AtomicInteger()
        val cdl = CountDownLatch(threadNum)
        val lock = ReentrantLock()

        val start = System.currentTimeMillis()
        for(i in 0..threadNum-1) {
            thread {
                while(true) {
                    lock.lock()
                    try {
                        if (counter.get() == 100000000) {
                            cdl.countDown();
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

        cdl.await()

        System.out?.println(threadNum.toString() + " " + (System.currentTimeMillis() - start));

        if(threadNum < 2 * processors)
            threadNum = threadNum + 1
        else
            threadNum = threadNum * 2
    }
}
