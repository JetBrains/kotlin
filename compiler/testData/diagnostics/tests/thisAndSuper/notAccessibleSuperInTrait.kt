open class A {
    open fun foo() {}
}

trait ATrait : A {

    override fun foo() {
        <!SUPERCLASS_NOT_ACCESSIBLE_FROM_TRAIT!>super<A><!>.foo()
    }
}