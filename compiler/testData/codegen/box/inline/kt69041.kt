// Should be fixed in JS as side effect of KT-74384, in WASM as side effect of KT-74392
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// SKIP_IR_DESERIALIZATION_CHECKS
// ^^^ After KT-70054 it is not possible to properly deserialize null dispatch receiver argument in calls, which occures in this test.
// It will be resolved in KT-74635.

class A {
    inline fun <reified T> foo(x: T) = x
}

fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}