package test

impl fun foo(<caret>n: Int) {
    n + 1
}

fun test() {
    foo(1)
    foo(n = 1)
}