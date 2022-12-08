// IGNORE_INLINER: IR
// WITH_STDLIB

fun g(b: (Int, (Int) -> String) -> Array<String>): Array<String> =
    b(1) { "O" }

inline fun h(b: (Int, (Int) -> String) -> Array<String>): Array<String> =
    b(1) { "K" }

fun box(): String = g(::Array)[0] + h(::Array)[0]
