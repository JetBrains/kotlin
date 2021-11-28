// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// WITH_STDLIB

inline fun foo(mkString: (Char, Char) -> String): String =
        mkString('O','K')

fun bar (vararg xs: Char) =
        xs.concatToString()

fun box(): String = foo(::bar)
// -> { a, b -> bar(a, b) }
