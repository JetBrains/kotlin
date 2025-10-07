// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

open class A {
    fun f(x: Any) {
    }
}

interface B<T> {
    fun f(x: T) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class C : B<String>, A()<!>
