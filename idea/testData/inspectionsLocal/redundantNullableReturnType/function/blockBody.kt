// PROBLEM: 'foo' always returns non-null type
// WITH_RUNTIME
fun foo(xs: List<Int>): Int?<caret> {
    return xs.first()
}