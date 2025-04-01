// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-13712
// SCOPE_DUMP: B:compareTo

open class A() {
    open fun compareTo(other: Any): Int = -1
}
class B() : A(), Comparable<B> {
    override fun compareTo(other: B): Int = 0
}

fun test() {
    B().compareTo(Any())
}
