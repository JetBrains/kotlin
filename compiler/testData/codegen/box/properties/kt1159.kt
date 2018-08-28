// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
