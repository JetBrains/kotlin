// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: +NewInference
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

inline fun foo(mkString: (Char, Char) -> String): String =
        mkString('O','K')

fun bar (vararg xs: Char) =
        String(xs)

fun box(): String = foo(::bar)
// -> { a, b -> bar(a, b) }