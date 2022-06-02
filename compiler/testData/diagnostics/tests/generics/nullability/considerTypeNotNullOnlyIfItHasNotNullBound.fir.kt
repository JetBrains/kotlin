
inline fun <T, reified S> foo(x: T?, y: T): T {
    if (x is S) return <!RETURN_TYPE_MISMATCH!>x<!>
    return y
}
