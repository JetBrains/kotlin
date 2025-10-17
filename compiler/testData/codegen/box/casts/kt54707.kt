// FILE: main.kt
fun box(): String =
    g(arrayOf("O"))

fun g(x: Array<String>?): String =
    x.orEmpty0().f { it + "K" }

// FILE: lib.kt
inline fun <T> Array<out T>.f(lambda: (T) -> T): T =
    lambda(this[0])

inline fun <reified T> Array<out T>?.orEmpty0(): Array<out T> =
    this ?: (arrayOfNulls<T>(0) as Array<T>)
