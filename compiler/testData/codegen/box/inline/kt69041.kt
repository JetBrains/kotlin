// Should be fixed in WASM as side effect of KT-74392
// IGNORE_BACKEND: WASM

class A {
    inline fun <reified T> foo(x: T) = x
}

fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}
