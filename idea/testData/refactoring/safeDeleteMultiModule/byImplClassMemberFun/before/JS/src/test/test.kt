package test

impl class Foo {
    impl fun <caret>foo(n: Int) {

    }
}

fun test(f: Foo) {
    f.foo(1)
}