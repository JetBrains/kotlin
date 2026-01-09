// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline fun f(getString: () -> String = { "OK" }) = getString()
inline fun g() { }

// FILE: main.kt
fun box(): String {
    g()
    return f()
}