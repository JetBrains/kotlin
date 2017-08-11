package test

impl class Foo {
    impl fun foo() {
        n + 1
    }
}

fun test(f: Foo) {
    f.foo()
    f.foo()
}