// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, WASM
@file:Suppress("INCOMPATIBLE_TYPES", "UNCHECKED_CAST")

fun <T> unchecked(x: Any?) = x as T

fun box() : String{
    if (unchecked<String>(null) is Any) return "FAIL 1"
    if (unchecked<String>(null) is IntArray) return "FAIL 2"
    try {
        unchecked<String>(null) as Any
        return "FAIL 3"
    } catch (e: NullPointerException) {
    }
    try {
        unchecked<String>(null) as IntArray
        return "FAIL 4"
    } catch (e: NullPointerException) {
    }
    return "OK"
}
