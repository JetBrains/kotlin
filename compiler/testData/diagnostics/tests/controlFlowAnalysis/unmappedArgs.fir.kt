fun foo(a: Int, b: Int) = a + b

fun bar(i: Int) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, 1, i)
}
