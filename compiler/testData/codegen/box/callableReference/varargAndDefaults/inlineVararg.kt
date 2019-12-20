// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS

inline fun foo(mkString: (Char, Char) -> String): String =
        mkString('O','K')

fun bar (vararg xs: Char) =
        String(xs)

fun box(): String = foo(::bar)
// -> { a, b -> bar(a, b) }