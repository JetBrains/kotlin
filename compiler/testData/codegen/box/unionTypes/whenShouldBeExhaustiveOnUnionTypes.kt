// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +UnionTypes

var b = false

fun box(): String {
    val x = if (b) {
        11L
    } else {
        "OK"
    }
    return when (x) {
        is Long -> "Fail"
        is String -> x
    }
}