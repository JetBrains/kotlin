public object RefreshQueue {
    private val workerThread: Thread = Thread(object : Runnable {
        override fun run() {
            workerThread.isInterrupted()
        }
    });

    init {
        workerThread.start()
    }
}

fun box() : String {
    RefreshQueue
    return "OK"
}
