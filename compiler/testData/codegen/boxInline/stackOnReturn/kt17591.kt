// FILE: 1.kt
inline fun alwaysOk(s: String, fn: (String) -> String): String {
    return fn(return "OK")
}

// FILE: 2.kt
fun box() = alwaysOk("what?") { it }