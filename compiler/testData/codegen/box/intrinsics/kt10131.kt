// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun box(): String =
        listOf('O', 'K').fold("", String::plus)
