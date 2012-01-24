import java.util.concurrent.ConcurrentLinkedQueue
import java.util.List

public object RefreshQueue {
    private val queue = ConcurrentLinkedQueue<List<String>>

    private val workerThread = Thread(object : Runnable {
        override fun run() {
                while (!workerThread.isInterrupted()) {
                    try {
//                        synchronized(queue) {
//                            queue.wait()
//                        }
                    } catch (e : InterruptedException) {
                    }
                }
        }
    })
}

fun box() : String {
    val t = RefreshQueue.workerThread
    return "OK"
}