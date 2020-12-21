fun foo(f: () -> Unit) {}
fun bar(x: Int, f: () -> Unit) {}
fun baz(f: () -> Unit, other: Boolean = true) {}


fun test() {
    // OK
    foo {}
    foo() {}
    foo({})

    // Bad
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>foo<!>(1) {}<!>
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>foo<!>(f = {}) {}<!>

    // OK
    bar(1) {}
    bar(x = 1) {}
    bar(1, {})
    bar(x = 1, f = {})

    // Bad
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>bar<!> {}<!>
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>bar<!>({})<!>

    // OK
    baz(other = false, f = {})
    baz({}, false)

    // Bad
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>baz<!> {}<!>
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>baz<!>() {}<!>
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>baz<!>(other = false) {}<!>
}
