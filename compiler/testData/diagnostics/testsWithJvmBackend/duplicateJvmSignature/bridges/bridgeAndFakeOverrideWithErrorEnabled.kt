// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: +ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
    fun acceptOpen(obj: T)
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
    open fun acceptOpen(obj: Restrict) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR, ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class Bar : Foo(), RestrictedGeneric<Bar>{
    override fun accept(obj: Bar): Int = 0
    override fun acceptOpen(obj: Bar) {
    }
}<!>
