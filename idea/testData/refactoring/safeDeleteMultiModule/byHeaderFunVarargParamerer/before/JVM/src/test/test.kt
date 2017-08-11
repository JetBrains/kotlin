package test

impl fun foo(s: String, vararg n: Int) {
    n.size
}

fun test() {
    foo("1", 2, 3)
}