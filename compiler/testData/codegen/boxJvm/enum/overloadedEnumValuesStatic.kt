// TARGET_BACKEND: JVM
// WITH_STDLIB

enum class E {
    A;

    companion object {
        @JvmStatic
        fun values(): Array<String> = arrayOf("OK")

        @JvmStatic
        fun E.values(): Array<E> = arrayOf(A)

        @JvmStatic
        fun values(x: Int): Array<E> = arrayOf(A)
    }
}

fun f(e: E) = when (e) {
    E.A -> "OK"
}

fun box(): String {
    return f(E.A)
}
