// IGNORE_NATIVE: optimizationMode=OPT
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

class Data(val x: Int)

fun box(): String {
    val arr = arrayOf("zzz")
    try {
        val x = (arr as Array<Data>)[0].x
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
