// WITH_STDLIB
// FILE: lib.kt

inline fun foo(mkString: () -> String): String =
        mkString()

// FILE: main.kt
fun bar (xs: CharArray = charArrayOf('O','K')) =
        xs.concatToString()

fun box(): String = foo(::bar)
