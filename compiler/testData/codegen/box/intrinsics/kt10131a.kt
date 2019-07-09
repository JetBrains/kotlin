// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String =
        charArrayOf('O', 'K').fold("", String::plus)
