fun test1(f: String.() -> Unit) {
    (<!INAPPLICABLE_CANDIDATE!>f<!>)()

    <!INAPPLICABLE_CANDIDATE!>f<!>()
}

fun test2(f: (Int) -> Int) {
    1.<!UNRESOLVED_REFERENCE!>f<!>(2)

    2.(<!UNRESOLVED_REFERENCE!>f<!>)(2)
}