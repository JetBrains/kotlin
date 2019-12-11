package a

fun test(c: C) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(c.b)
}

fun foo(s: String) = s

class C(val b: Int) {}