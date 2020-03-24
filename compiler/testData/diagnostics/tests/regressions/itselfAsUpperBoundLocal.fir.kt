// !WITH_NEW_INFERENCE
fun bar() {
    fun <T: T?> foo() {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>()
}
