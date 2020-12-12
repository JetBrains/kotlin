// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun interface KRunnable {
    fun invoke()
}

object OK : () -> Unit {
    override fun invoke() {
        result = "OK"
    }
}

fun foo(k: KRunnable) {
    k.invoke()
}

var result: String = ""

fun box(): String {
    foo(OK)
    return result
}