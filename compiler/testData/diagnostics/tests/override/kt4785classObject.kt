trait A {
    internal fun foo()
}

trait B {
    protected fun foo() {}
}

class C {
    <!CANNOT_INFER_VISIBILITY!>class object<!> : A, B {
        fun bar() = null
    }
}
