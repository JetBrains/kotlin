fun foo(s: String) {
    s.
    <!ILLEGAL_SELECTOR!>val b = 42<!>
}