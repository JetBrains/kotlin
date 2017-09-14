package test

impl fun foo(n: Int) {
    n + 1
}

fun test() {
    foo(1)
    foo(n = 1)
}