// !LANGUAGE: +SuspendConversion
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun foo(s: SuspendRunnable) {}

fun test(f: () -> Unit) {
    foo { }
    foo(f)
}

fun box(): String {
    test({ "" })
    return "OK"
}