// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String =
        if (listOf("abc", "de", "f").map(String::length) == listOf(3, 2, 1)) "OK" else "Fail"
