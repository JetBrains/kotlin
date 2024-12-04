// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// TODO: Consider rewriting this test without using threads, since the issue is not about threads at all.

object RefreshQueue {
    val any = Any()
    val workerThread: Thread = Thread(object : Runnable {
        override fun run() {
            val a = any
            val b = RefreshQueue.any
            if (a != b) throw AssertionError()
        }
    })
}

fun box() : String {
    RefreshQueue.workerThread.run()
    return "OK"
}
