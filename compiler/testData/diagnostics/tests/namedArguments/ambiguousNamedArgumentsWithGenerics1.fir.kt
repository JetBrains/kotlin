interface A<T> {
    fun foo(a: T)
}

interface B {
    fun foo(b: Int)
}

interface C : A<Int>, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(a = 1)
    c.<!INAPPLICABLE_CANDIDATE!>foo<!>(b = 1)
}