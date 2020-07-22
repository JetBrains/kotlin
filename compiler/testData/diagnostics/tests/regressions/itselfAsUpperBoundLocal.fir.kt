// !WITH_NEW_INFERENCE
fun bar() {
    fun <T: <!OTHER_ERROR!>T?<!>> foo() {}
    <!INAPPLICABLE_CANDIDATE!>foo<!>()
}
