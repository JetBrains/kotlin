// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect enum class E {
    O, K;

    fun values(a: Int): Int
}

fun common(): String {
    return if (E.O.values(42) == 42) E.valueOf("O").name + E.valueOf("K").name else "NOT OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual enum class E {
    O, K;

    // Not to be confused with `static fun values`
    actual fun values(a: Int): Int = a
}

fun box(): String = common()
