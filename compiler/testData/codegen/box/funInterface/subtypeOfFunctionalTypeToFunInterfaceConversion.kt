// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

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