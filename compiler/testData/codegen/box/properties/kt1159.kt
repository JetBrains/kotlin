// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
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
