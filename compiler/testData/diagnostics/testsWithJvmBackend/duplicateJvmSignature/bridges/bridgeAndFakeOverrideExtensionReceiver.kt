// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

interface A<T> {
    fun T.foo() {}
}

open class B1 {
    fun Any.foo() {}
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class B2 : A<String>, B1() {
    override fun String.foo() {}
}<!>

interface C1 {
    fun Any.foo() {}
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class C2 : A<String>, C1 {
    override fun String.foo() {}
}<!>
