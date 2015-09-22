interface T {
    fun foo()
}

open class C {
    protected fun foo() {}
}

class <!CANNOT_INFER_VISIBILITY!>E<!> : C(), T

val z: T = <!CANNOT_INFER_VISIBILITY!>object<!> : C(), T {}
