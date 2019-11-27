// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String =
        if (listOf("abc", "de", "f").map(String::length) == listOf(3, 2, 1)) "OK" else "Fail"
