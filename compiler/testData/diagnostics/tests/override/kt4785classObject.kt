interface A {
    internal fun foo()
}

interface B {
    protected fun foo() {}
}

class C {
    companion <!CANNOT_INFER_VISIBILITY!>object<!> : A, B {
        fun bar() = null
    }
}
