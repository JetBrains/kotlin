public object RefreshQueue {
    private val workerThread: Thread = Thread(object : Runnable {
        override fun run() {
            workerThread.isInterrupted()
        }
    });

    {
        workerThread.start()
    }
}

fun box() : String {
    RefreshQueue
    return "OK"
}
