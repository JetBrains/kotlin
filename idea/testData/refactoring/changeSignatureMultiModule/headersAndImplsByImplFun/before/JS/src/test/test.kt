package test

impl fun foo() { }
impl fun <caret>foo(n: Int) { }
impl fun bar(n: Int) { }

fun test() {
    foo()
    foo(1)
    bar(1)
}