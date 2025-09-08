// FILE: lib.kt
fun zap(s: String): String? = s

inline fun tryZap(s: String, fn: (String) -> String): String {
    return fn(zap(s) ?: return "null")
}

// FILE: main.kt
fun box() = tryZap("OK") { it }