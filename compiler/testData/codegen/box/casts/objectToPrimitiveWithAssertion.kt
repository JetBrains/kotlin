// IGNORE_BACKEND: JS, JS_IR, WASM
@Suppress("UNCHECKED_CAST")
fun <T> f() = 1L as T

fun box(): String {
    val x: Int = f()!! // T = Int?, but the cast succeeds because it's immediately upcasted to Number
    return if (x == 1) "OK" else "fail: $x"
}
