// Should be fixed in WASM as side effect of KT-74392
// IGNORE_BACKEND: WASM
// IGNORE_NATIVE: compatibilityTestMode=FORWARD
// ^^^ This new test fails under 2.1.0 compiler and passes on 2.2.0 and later


inline val <reified T> T.id: T
    get() = (this as Any) as T

fun foo(x: (String) -> String) = x("OK")

fun box(): String {
    return foo(String::id)
}
