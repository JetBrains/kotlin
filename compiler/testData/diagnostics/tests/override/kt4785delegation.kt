trait A {
    internal fun foo()
}

trait B {
    protected fun foo()
}

class <!CANNOT_INFER_VISIBILITY!>E(a: A)<!> : A by a, B
