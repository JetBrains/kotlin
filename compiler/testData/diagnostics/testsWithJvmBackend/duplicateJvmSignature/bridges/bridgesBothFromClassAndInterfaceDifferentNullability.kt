// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

open class A<T> {
    fun foo(t: T?) {
    }
}

interface B<T> {
    fun foo(t: T) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class C : A<String>(), B<String><!>
