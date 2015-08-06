// WITH_RUNTIME

interface I

fun f() = <caret>listOf(object : I { })
