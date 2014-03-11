fun foo(a: Boolean, b: Boolean, c: Boolean) {
    return <caret>!(a && b) || !(a || c)
}