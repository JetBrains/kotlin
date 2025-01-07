// Should be fixed in WASM as side effect of KT-74392
// IGNORE_BACKEND: WASM

inline val <reified T> T.id: T
    get() = (this as Any) as T

fun foo(x: (String) -> String) = x("OK")

fun box(): String {
    return foo(String::id)
}
