// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

class L<T>(var a: T) {}

fun foo() = L<Int>(5).a

fun box(): String {
    val x: Any = foo()
    return if (x is Integer) "OK" else "Fail $x"
}
