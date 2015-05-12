interface A {
    internal fun foo()
}

interface B {
    protected fun foo()
}

class <!CANNOT_INFER_VISIBILITY!>E(a: A)<!> : A by a, B
