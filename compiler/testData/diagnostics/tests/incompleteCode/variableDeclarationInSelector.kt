fun foo(s: String) {
    s.
    <!DECLARATION_IN_ILLEGAL_CONTEXT, ILLEGAL_SELECTOR!>val b = 42<!>
}