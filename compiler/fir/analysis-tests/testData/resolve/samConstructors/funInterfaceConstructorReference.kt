fun interface Test {
    fun foo()
}

val f = ::<!FUN_INTERFACE_CONSTRUCTOR_REFERENCE!>Test<!>
