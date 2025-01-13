// Should be fixed in JS as side effect of KT-74384, in WASM as side effect of KT-74392
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

class A {
    inline fun <reified T> foo(x: T) = x
}

fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}