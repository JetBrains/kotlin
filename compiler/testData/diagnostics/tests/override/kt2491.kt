interface T {
    public fun foo()
}

open class C {
    protected fun foo() {}
}

class <!CANNOT_INFER_VISIBILITY!>D<!> : C(), T

val obj: C = <!CANNOT_INFER_VISIBILITY!>object<!> : C(), T {}