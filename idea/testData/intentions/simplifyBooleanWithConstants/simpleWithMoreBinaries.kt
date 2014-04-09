fun foo(y: Boolean) {
    (y && false) || (y && y && true && (y && true))<caret> && false && true
}