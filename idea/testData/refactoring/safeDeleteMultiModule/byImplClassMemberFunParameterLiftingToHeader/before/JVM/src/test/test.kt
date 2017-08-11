package test

impl class Foo {
    impl fun foo(n: Int) {
        n + 1
    }
}

fun test(f: Foo) {
    f.foo(1)
    f.foo(n = 1)
}