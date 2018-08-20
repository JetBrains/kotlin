// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String =
        listOf('O', 'K').fold("", String::plus)
