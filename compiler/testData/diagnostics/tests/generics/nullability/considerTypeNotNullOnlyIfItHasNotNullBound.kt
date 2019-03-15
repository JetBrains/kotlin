// !LANGUAGE: +NewInference

inline fun <T, reified S> foo(x: T?, y: T): T {
    if (x is S) return <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    return y
}
