package test

impl class C {
    impl fun foo() { }
    impl fun baz(n: Int) { }
    impl fun bar(n: Int) { }
}

fun test(c: C) {
    c.foo()
    c.baz(1)
    c.bar(1)
}