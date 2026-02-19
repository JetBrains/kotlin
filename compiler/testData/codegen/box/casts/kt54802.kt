// FILE: lib.kt
class K {
    val x: String = "OK"
}

inline fun <T> Array<out T>.ifEmpty(body: () -> Array<out T>): Array<out T> =
    if (size == 0) body() else this

inline fun <T> Array<out T>.f(p: (T) -> String): String =
    p(this[0])

// FILE: main.kt
fun box(): String =
    emptyArray<K>()
        .ifEmpty { arrayOf(K()) }
        .f(K::x)
