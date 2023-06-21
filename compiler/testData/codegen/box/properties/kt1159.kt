// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

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
