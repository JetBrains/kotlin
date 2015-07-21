fun bar() {
    fun <T: <!CYCLIC_GENERIC_UPPER_BOUND!>T?<!>> foo() {}
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
}
