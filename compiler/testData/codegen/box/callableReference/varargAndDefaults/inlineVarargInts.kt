// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS

inline fun foo(x: (Int, Int) -> Int): Int =
    x(120,3)

fun bar(vararg x: Int): Int =
    x.sum()

fun box(): String =
    if (foo(::bar) == 123) "OK" else "FAIL"
