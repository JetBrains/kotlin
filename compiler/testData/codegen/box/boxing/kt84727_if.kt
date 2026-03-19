// IGNORE_BACKEND: JVM_IR, WASM_JS, WASM_WASI

@Suppress("UNCHECKED_CAST")
fun <T> uncheckedCastNull(): T = null as T

var flag = true

fun box(): String {
    if ((if (flag) { uncheckedCastNull<Int>() } else 0) != null) return "FAIL 1"

    val value: Int? = if (flag) { uncheckedCastNull<Int>() } else 0
    if (value != null) return "FAIL 2: $value"

    return "OK"
}
