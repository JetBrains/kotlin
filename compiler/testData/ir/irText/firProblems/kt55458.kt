// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// IGNORE_BACKEND_K1: NATIVE

fun runMe() {
    val (a: Any, _) = 1 to 2
    println(a)
}
