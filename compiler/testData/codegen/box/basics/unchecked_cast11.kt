// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

fun foo(x: Int) = x + 42

fun box(): String {
    val arr = arrayOf("zzz")
    try {
        val x = foo((arr as Array<Int>)[0])
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
