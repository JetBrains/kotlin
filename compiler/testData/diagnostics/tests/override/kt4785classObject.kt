trait A {
    internal fun foo()
}

trait B {
    protected fun foo() {}
}

class C {
    default <!CANNOT_INFER_VISIBILITY!>object<!> : A, B {
        fun bar() = null
    }
}
