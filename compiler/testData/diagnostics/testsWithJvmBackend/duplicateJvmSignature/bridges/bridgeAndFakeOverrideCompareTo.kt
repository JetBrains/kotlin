// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-13712

open class A() {
    open fun compareTo(other: Any): Int = 0
}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class B() : A(), Comparable<B> {
    override fun compareTo(other: B): Int = 0
}<!>

fun test() {
    B().compareTo(object { })
}
