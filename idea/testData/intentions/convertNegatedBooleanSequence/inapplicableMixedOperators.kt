// IS_APPLICABLE: false
fun foo(a: Boolean, b: Boolean, c: Boolean): Boolean {
    return <caret>!a && !b || !c
}
