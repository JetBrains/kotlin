// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR
// ^^^ This test fails due to visibility violation on access to JS `internal` intrinsic functions
//     `kotlin.sharedBoxCreate`, `kotlin.sharedBoxRead` and `kotlin.sharedBoxWrite`. To be fixed in KT-70295.

// IGNORE_INLINER_K2: IR

// FILE: A.kt
fun wrapper(block: () -> Unit) { block() }

private lateinit var o: String

class A {
    internal inline fun inlineMethod(): String {
        lateinit var k: String
        wrapper {
            o = "O"
            k = "K"
        }
        return o + k
    }
}

// FILE: main.kt
fun box() = A().inlineMethod()
