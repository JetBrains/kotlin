fun foo(f: () -> Unit) {}
fun bar(x: Int, f: () -> Unit) {}
fun baz(f: () -> Unit, other: Boolean = true) {}


fun test() {
    foo {}
    foo() {}
    foo({})

    <!INAPPLICABLE_CANDIDATE!>foo<!>(1) {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>(f = {}) {}

    bar(1) {}
    bar(x = 1) {}
    bar(1, {})
    bar(x = 1, f = {})

    <!INAPPLICABLE_CANDIDATE!>bar<!> {}
    <!INAPPLICABLE_CANDIDATE!>bar<!>({})

    baz(other = false, f = {})
    baz({}, false)

    <!INAPPLICABLE_CANDIDATE!>baz<!> {}
    <!INAPPLICABLE_CANDIDATE!>baz<!>() {}
    <!INAPPLICABLE_CANDIDATE!>baz<!>(other = false) {}
}