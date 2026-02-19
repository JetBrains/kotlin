// FILE: lib.kt
fun zap(s: String) = s

inline fun tryZap(string: String, fn: (String) -> String) =
        fn(try { zap(string) } finally {})

// FILE: main.kt
fun box(): String = tryZap("OK") { it }