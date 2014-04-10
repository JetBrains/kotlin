fun foo(y: Boolean) {
    y || false && true || <caret>false || false || false || y && true
}