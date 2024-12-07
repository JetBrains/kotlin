// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// WITH_STDLIB

class Data(val x: Int)

fun box(): String {
    val arr = arrayOf("zzz")
    try {
        var x: Int = 0
        (arr as Array<Data>).forEach {
            x = it.x
        }
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
