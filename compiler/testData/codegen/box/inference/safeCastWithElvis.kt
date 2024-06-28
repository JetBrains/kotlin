// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM, NATIVE
// ISSUE: KT-62806

fun <T: Number> foo(x: Number) = x as? T ?: TODO()
val x: Int? = foo<Int>(1)

fun box(): String {
    return if (x == 1) "OK" else "Fail: $x"
}
