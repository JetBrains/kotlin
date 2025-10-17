// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO
@Suppress("UNCHECKED_CAST")
fun <T> f() = 1L as T

fun box(): String {
    val x: Int = f()!! // T = Int?, but the cast succeeds because it's immediately upcasted to Number
    return if (x == 1) "OK" else "fail: $x"
}
