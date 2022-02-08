fun bar() {
    fun <T: <!UNRESOLVED_REFERENCE!>T<!>?> foo() {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>()
}
