// ISSUE: KT-68975
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
// FILE: lib.kt
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, noinline makeString: () -> String): String {
    return js("p(arg, makeString)")
}

// FILE: main.kt
fun box() = foo("O") { "K" }
