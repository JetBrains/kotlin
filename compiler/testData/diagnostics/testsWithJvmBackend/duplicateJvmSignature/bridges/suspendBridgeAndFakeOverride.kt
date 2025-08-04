// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    suspend fun accept(obj: T): Int
    suspend fun acceptOpen(obj: T)
}

open class Foo {
    suspend fun accept(obj: Restrict): Int = 0
    open suspend fun acceptOpen(obj: Restrict) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING, ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class Bar : Foo(), RestrictedGeneric<Bar>{
    override suspend fun accept(obj: Bar): Int = 0
    override suspend fun acceptOpen(obj: Bar) {
    }
}<!>
