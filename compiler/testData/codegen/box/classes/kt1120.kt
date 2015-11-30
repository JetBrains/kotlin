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
