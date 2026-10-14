// IGNORE_BACKEND: WASM_JS, WASM_WASI
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4.20
// KT-49422: Fixed in 2.5.0-Beta2

fun <T> test(t: T) = t as (T & Any)

fun box(): String =
    try {
        test<Any?>(null)
        "FAIL: expected NPE"
    } catch (ex: NullPointerException) {
        test("OK")
    }
