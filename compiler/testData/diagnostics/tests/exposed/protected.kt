open class A {
    // protected relative to A
    protected open class B { 
        fun foo() {}
    }
    public open class C {
        // protected relative to C, must be an error
        protected open class D : <!EXPOSED_SUPER_CLASS!>B()<!>
    }
}

class E : A.C() {
    // F has invisible grandparent class B (E does not inherit from A)
    class F : <!EXPOSED_SUPER_CLASS!>A.C.D()<!> {
        init {
            // Invoke function from invisible grandparent
            foo() 
        }
    }
}