// IGNORE_BACKEND: JVM

inline fun g(h: () -> String): String = h()

fun box(): String {
    val result = "OK"
    fun Any.f(): String = result
    return g("Fail"::f)
}
