// Should be fixed in WASM as side effect of KT-74392
// IGNORE_BACKEND: WASM
// IGNORE_NATIVE: compatibilityTestMode=FORWARD
// ^^^ This new test fails under 2.1.0 compiler with IndexOutOfBoundsException and passes on 2.2.0 and later

class A {
    inline fun <reified T> foo(x: T) = x
}

fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}
