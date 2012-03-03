/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lockperf_kotlin

import kotlin.io.*
import kotlin.util.*
import kotlin.concurrent.*

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
                                    countDown()
                                    break;
                                } else {
                                    counter.incrementAndGet()
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

        if(threadNum < 2 * processors) threadNum++ else threadNum = 2*threadNum
    }
}
