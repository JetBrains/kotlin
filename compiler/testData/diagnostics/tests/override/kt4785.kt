// ISSUES: KT-2491, KT-4785, KT-63741, KT-59400

interface T {
    fun foo()
}

open class C {
    protected fun foo() {}
}

class <!CANNOT_INFER_VISIBILITY!>E<!> : C(), T

val z: T = <!CANNOT_INFER_VISIBILITY!>object<!> : C(), T {}
