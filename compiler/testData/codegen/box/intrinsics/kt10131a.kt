// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String =
        charArrayOf('O', 'K').fold("", String::plus)
