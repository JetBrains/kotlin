// LANGUAGE: +DefinitelyNonNullableTypes
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: WASM_JS, WASM_WASI

fun <T> test(t: T) = t as (T & Any)

fun box(): String =
    try {
        test<Any?>(null)
        "FAIL: expected NPE"
    } catch (ex: NullPointerException) {
        test("OK")
    }