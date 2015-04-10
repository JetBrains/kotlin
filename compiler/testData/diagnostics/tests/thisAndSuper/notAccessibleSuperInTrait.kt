open class A {
    open fun foo() {}
}

trait ATrait : <!TRAIT_WITH_SUPERCLASS!>A<!> {

    override fun foo() {
        <!SUPERCLASS_NOT_ACCESSIBLE_FROM_TRAIT!>super<A><!>.foo()
    }
}