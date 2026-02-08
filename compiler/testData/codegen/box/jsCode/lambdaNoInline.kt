// ISSUE: KT-68975
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// KJS_WITH_FULL_RUNTIME
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, noinline makeString: () -> String): String {
    return js("p(arg, makeString)")
}

fun box() = foo("O") { "K" }
