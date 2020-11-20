// !WITH_NEW_INFERENCE
fun bar() {
    fun <T: <!UNRESOLVED_REFERENCE!>T?<!>> foo() {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>()
}
