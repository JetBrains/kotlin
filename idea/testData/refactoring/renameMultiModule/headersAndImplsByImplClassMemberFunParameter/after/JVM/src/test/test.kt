package test

impl class C {
    impl fun foo(x: Int) {

    }
}

fun test(c: C) {
    c.foo(1)
    c.foo(x = 1)
}