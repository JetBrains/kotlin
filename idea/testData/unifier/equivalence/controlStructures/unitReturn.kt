// DISABLE-ERRORS
fun foo(a: Int) {
    if (a > 0) <selection>return</selection>
    if (a < 0) return a
    return
}