// !LANGUAGE: +DefinitelyNonNullableTypes
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: WASM

fun <T> test(t: T) = t as (T & Any)

fun box(): String =
    try {
        test<Any?>(null)
        "FAIL: expected NPE"
    } catch (ex: NullPointerException) {
        test("OK")
    }