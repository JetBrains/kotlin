// ISSUES: KT-2491, KT-4785, KT-63741, KT-59400

interface T {
    fun foo()
}

open class C {
    protected fun foo() {}
}

class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>E<!> : C(), T

val z: T = <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>object<!> : C(), T {}
