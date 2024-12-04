// WITH_STDLIB

inline fun foo(mkString: () -> String): String =
        mkString()

fun bar (xs: CharArray = charArrayOf('O','K')) =
        xs.concatToString()

fun box(): String = foo(::bar)
