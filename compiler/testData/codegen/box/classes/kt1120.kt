// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
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
