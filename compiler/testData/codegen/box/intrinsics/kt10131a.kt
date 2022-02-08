// WITH_STDLIB

fun box(): String =
        charArrayOf('O', 'K').fold("", String::plus)
