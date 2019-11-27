// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// IGNORE_BACKEND: JS_IR

inline fun foo(mkString: () -> String): String =
        mkString()

fun bar (xs: CharArray = charArrayOf('O','K')) =
        String(xs)

fun box(): String = foo(::bar)
