package test

impl fun baz() { }
impl fun baz(n: Int) { }
impl fun bar(n: Int) { }

fun test() {
    baz()
    baz(1)
    bar(1)
}