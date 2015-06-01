fun bar() {
    fun <T: <!CYCLIC_GENERIC_UPPER_BOUND!>T?<!>> foo() {}
    foo()
}
