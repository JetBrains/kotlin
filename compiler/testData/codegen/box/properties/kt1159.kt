public object RefreshQueue {

    private val any = Any()

    private val workerThread: Thread = Thread(object : Runnable {
        override fun run() {
            any.hashCode()
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
