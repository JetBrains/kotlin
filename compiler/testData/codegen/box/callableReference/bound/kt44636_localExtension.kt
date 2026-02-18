// FILE: lib.kt
inline fun g(h: () -> String): String = h()

// FILE: main.kt
fun box(): String {
    val result = "OK"
    fun Any.f(): String = result
    return g("Fail"::f)
}
