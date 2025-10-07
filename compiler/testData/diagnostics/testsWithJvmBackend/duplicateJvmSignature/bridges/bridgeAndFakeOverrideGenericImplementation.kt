// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class Bar<T> : Foo(), RestrictedGeneric<Bar<T>>{
    override fun accept(obj: Bar<T>): Int = 0
}<!>
