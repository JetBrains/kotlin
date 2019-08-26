// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
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

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ Thread 
