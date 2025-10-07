// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
}

open class Foo<D: Restrict> {
    fun accept(obj: D): Int = 0
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class Bar : Foo<EmptyRestrict>(), RestrictedGeneric<Bar> {
    override fun accept(obj: Bar): Int = 0
}<!>
