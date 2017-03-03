package test

impl fun foo() { }
impl fun baz(n: Int) { }
impl fun bar(n: Int) { }

fun test() {
    foo()
    baz(1)
    bar(1)
}