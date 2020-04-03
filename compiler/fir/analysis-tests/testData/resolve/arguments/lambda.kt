fun foo(f: () -> Unit) {}
fun bar(x: Int, f: () -> Unit) {}
fun baz(f: () -> Unit, other: Boolean = true) {}


fun test() {
    // OK
    foo {}
    foo() {}
    foo({})

    // Bad
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1) {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f = {}) {}

    // OK
    bar(1) {}
    bar(x = 1) {}
    bar(1, {})
    bar(x = 1, f = {})

    // Bad
    <!INAPPLICABLE_CANDIDATE!>bar<!> {}
    <!INAPPLICABLE_CANDIDATE!>bar<!>({})

    // OK
    baz(other = false, f = {})
    baz({}, false)

    // Bad
    <!INAPPLICABLE_CANDIDATE!>baz<!> {}
    <!INAPPLICABLE_CANDIDATE!>baz<!>() {}
    <!INAPPLICABLE_CANDIDATE!>baz<!>(other = false) {}
}