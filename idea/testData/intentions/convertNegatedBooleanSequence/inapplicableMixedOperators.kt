// IS_APPLICABLE: false
fun foo(a: Boolean, b: Boolean, c: Boolean) {
    return <caret>!a && !b || !c
}