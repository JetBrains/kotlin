// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun box(): String =
        if (listOf("abc", "de", "f").map(String::length) == listOf(3, 2, 1)) "OK" else "Fail"
