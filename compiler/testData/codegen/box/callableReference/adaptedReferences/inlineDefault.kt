// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// WITH_RUNTIME

inline fun foo(mkString: () -> String): String =
        mkString()

fun bar (xs: CharArray = charArrayOf('O','K')) =
        xs.concatToString()

fun box(): String = foo(::bar)
