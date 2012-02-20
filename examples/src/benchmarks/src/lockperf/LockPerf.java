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

package lockperf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LockPerf {
    public static void main(String[] args) {
        int processors = Runtime.getRuntime().availableProcessors();
        for (int threadNum = 1; threadNum <= 1024; threadNum = threadNum < 2 * processors ? threadNum + 1 : threadNum * 2) {
            final AtomicInteger counter = new AtomicInteger();
            final CountDownLatch cdl = new CountDownLatch(threadNum);

            final ReentrantLock lock = new ReentrantLock();

            long start = System.currentTimeMillis();
            for (int i = 0; i < threadNum; ++i) {
                new Thread(new Runnable() {
                    public void run() {
                        for (;;) {
                            lock.lock();
                            try {
                                if (counter.get() == 100000000) {
                                    cdl.countDown();
                                    break;
                                } else {
                                    counter.incrementAndGet();
                                }
                            } finally {
                                lock.unlock();
                            }
                        }

                    }
                }).start();
            }

            try {
                cdl.await();
            } catch (InterruptedException e) {//
            }
            System.out.println(threadNum + " " + (System.currentTimeMillis() - start));
        }
    }
}
