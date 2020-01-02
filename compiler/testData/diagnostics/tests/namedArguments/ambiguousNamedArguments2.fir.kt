interface A {
    fun foo(a1: Int, a2: Double)
    fun bar(a1: Int, a2: Double, a3: String)
    fun baz(a1: Int, a2: Double, a3: String, a4: Int, a5: String)
}

interface B {
    fun foo(b1: Int, b2: Double)
    fun bar(a1: Int, a2: Double, b3: String)
    fun baz(a1: Int, b2: Double, a3: String, b4: Int, a5: String)
}

interface C : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.<!INAPPLICABLE_CANDIDATE!>foo<!>(b1 = 1, b2 = 1.0)
    c.<!INAPPLICABLE_CANDIDATE!>foo<!>(a1 = 1, b2 = 1.0)
    c.foo(a1 = 1, a2 = 1.0)
    c.foo(a1 = 1, a2 = 1.0)
    c.<!INAPPLICABLE_CANDIDATE!>bar<!>(a1 = 1, a2 = 1.0, b3= "")
    c.<!INAPPLICABLE_CANDIDATE!>baz<!>(a1 = 1, b2 = 1.0, a3 = "", b4 = 2, a5 = "")
    c.<!INAPPLICABLE_CANDIDATE!>baz<!>(a1 = 1, a2 = 1.0, a3 = "", b4 = 2, a5 = "")
}