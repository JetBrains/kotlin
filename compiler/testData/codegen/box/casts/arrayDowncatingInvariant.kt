// TARGET_BACKEND: JVM

// FILE: lib.kt
inline fun <reified T : CharSequence> f(x: Array<Any>) = x as Array<T>

// FILE: main.kt
fun box(): String = try {
    f<String>(arrayOf<Any>(42))
    "Fail"
} catch (e: Exception) {
    "OK"
}