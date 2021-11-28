// FILE: 1.kt
inline fun runReturning(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
fun box() = Array<String>(1) { runReturning { return@Array "OK" } }[0]
