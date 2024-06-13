// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

fun <T> foo(x: Int = 0): T = Any() as T

fun box(): String {
    try {
        val s = foo<String>()
        return "FAIL: ${s.length}"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
