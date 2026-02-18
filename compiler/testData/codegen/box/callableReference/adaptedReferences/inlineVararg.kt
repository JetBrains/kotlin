// WITH_STDLIB
// FILE: lib.kt

inline fun foo(mkString: (Char, Char) -> String): String =
        mkString('O','K')

// FILE: main.kt

fun bar (vararg xs: Char) =
        xs.concatToString()

fun box(): String = foo(::bar)
// -> { a, b -> bar(a, b) }
