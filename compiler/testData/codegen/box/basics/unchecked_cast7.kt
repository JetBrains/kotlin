// IGNORE_NATIVE: optimizationMode=OPT
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

fun box(): String {
    try {
        val x = Any().uncheckedCast<Int?>()
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}

fun <T> Any?.uncheckedCast(): T = this as T