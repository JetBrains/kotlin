namespace lockperformance

import std.io.*
import std.util.*
import std.concurrent.*

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

fun <T> Int.latch(op:  CountDownLatch.() -> T) : T {
    val cdl = CountDownLatch(this)
    val res = cdl.op()
    cdl.await()
    return res
}

fun main(args: Array<String>) {
    val processors = Runtime.getRuntime().sure().availableProcessors()
    var threadNum = 1
    while(threadNum <= 1024) {
        val counter = AtomicInteger()

        val duration = measureTimeMillis {
            threadNum.latch{
                val lock = ReentrantLock()
                for(i in 0..threadNum-1) {
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
        }

        println(threadNum.toString() + " " + duration)

        if(threadNum < 2 * processors)
            threadNum = threadNum + 1
        else
            threadNum = threadNum * 2
    }
}
