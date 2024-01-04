// IGNORE_NATIVE: optimizationMode=OPT
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

class Data(val x: Int)

fun box(): String {
    val arr = arrayOf("zzz")
    try {
        var x: Int = 0
        for (it in (arr as Array<Data>)) {
            x = it.x
        }
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
