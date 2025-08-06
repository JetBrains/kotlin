// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface Restrict

abstract class RestrictedGeneric<T: Restrict>: Restrict {
    abstract fun accept(obj: T): Int
}

interface Foo {
    open fun accept(obj: Restrict): Int = 0
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class Bar : Foo, RestrictedGeneric<Bar>() {
    override fun accept(obj: Bar): Int = 0
}<!>
