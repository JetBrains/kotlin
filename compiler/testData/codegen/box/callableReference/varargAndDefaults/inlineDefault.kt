// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

inline fun foo(mkString: () -> String): String =
        mkString()

fun bar (xs: CharArray = charArrayOf('O','K')) =
        String(xs)

fun box(): String = foo(::bar)
