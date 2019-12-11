open class A {
    // protected relative to A
    protected open class B { 
        fun foo() {}
    }
    public open class C {
        // protected relative to C, must be an error
        protected open class D : B()
    }
}

class E : A.C() {
    // F has invisible grandparent class B (E does not inherit from A)
    class F : A.C.D() {
        init {
            // Invoke function from invisible grandparent
            foo() 
        }
    }
}