// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

open class A() {
    open fun compareTo(other: Any): Int = 0
}
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class B() : A(), Comparable<B> {
    override fun compareTo(other: B): Int = 0
}<!>

fun test() {
    B().compareTo(object { })
}
