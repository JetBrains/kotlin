// IGNORE_BACKEND: WASM_JS, WASM_WASI

@Suppress("UNCHECKED_CAST")
fun <T> uncheckedCastNull(): T = null as T

inline fun inlineUncheckedCastNullToInt(): Int? = uncheckedCastNull<Int>()

fun box(): String {
    if (inlineUncheckedCastNullToInt() != null) return "FAIL 1"

    val value = inlineUncheckedCastNullToInt()
    if (value != null) return "FAIL 2: $value"

    return "OK"
}
