package test

impl class C {
    impl fun /*rename*/foo() { }
    impl fun foo(n: Int) { }
    impl fun bar(n: Int) { }
}

fun test(c: C) {
    c.foo()
    c.foo(1)
    c.bar(1)
}