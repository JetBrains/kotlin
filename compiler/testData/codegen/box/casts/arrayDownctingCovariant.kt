// TARGET_BACKEND: JVM

inline fun <reified T : CharSequence> f(x: Array<out Any>) = x as Array<T>

fun box(): String = try {
    f<String>(arrayOf<Int>(42))
    "Fail"
} catch (e: Exception) {
    "OK"
}