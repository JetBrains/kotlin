// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// Won't ever work with JS backend.
// TODO: Consider rewriting this test without using threads, since the issue is not about threads at all.
// IGNORE_BACKEND: JS, NATIVE

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
