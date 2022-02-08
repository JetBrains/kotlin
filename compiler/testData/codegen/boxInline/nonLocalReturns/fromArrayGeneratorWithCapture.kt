// FILE: 1.kt
inline fun runReturning(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
fun box(): String {
    val result = "OK"
    return Array<String>(1) { run { runReturning { return@Array result } } }[0]
}
