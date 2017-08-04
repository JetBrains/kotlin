package test

impl class C {
    impl fun baz() { }
    impl fun baz(n: Int) { }
    impl fun bar(n: Int) { }
}

fun test(c: C) {
    c.baz()
    c.baz(1)
    c.bar(1)
}