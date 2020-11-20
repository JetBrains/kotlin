open class A {
    open fun foo() {}
}

interface ATrait : <!INTERFACE_WITH_SUPERCLASS!>A<!> {
    override fun foo() {
        <!SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE!>super<A><!>.foo()
    }
}