// Should be fixed in JS as side effect of KT-74384, in WASM as side effect of KT-74392
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

inline val <reified T> T.id: T
    get() = (this as Any) as T

fun foo(x: (String) -> String) = x("OK")

fun box(): String {
    return foo(String::id)
}