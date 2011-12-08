import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
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
