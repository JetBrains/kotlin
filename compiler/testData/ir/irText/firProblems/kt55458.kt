// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// IGNORE_BACKEND_K1: NATIVE

fun main() {
    val (a: Any, _) = 1 to 2
    println(a)
}
