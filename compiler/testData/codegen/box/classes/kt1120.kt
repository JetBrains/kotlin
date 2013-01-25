object RefreshQueue {
    val workerThread: Thread = Thread(object : Runnable {
        override fun run() {
            val a = workerThread
            val b = RefreshQueue.workerThread
            if (a != b) throw AssertionError()
        }
    })
}

fun box() : String {
    RefreshQueue.workerThread.run()
    return "OK"
}
