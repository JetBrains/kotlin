// PROBLEM: 'foo' always returns non-null type
fun foo(i: Int?): Int?<caret> {
    if (i != null) {
        return i
    } else {
        return 0
    }
}
