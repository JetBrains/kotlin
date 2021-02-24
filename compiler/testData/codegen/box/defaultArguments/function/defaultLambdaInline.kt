inline fun f(getString: () -> String = { "OK" }) = getString()
inline fun g() { }

fun box(): String {
    g()
    return f()
}