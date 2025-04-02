// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-13712
// LANGUAGE: +ForbidOverloadClashesByErasure
// SCOPE_DUMP: B:compareTo

open class A() {
    open fun compareTo(other: Any): Int = -1
}
class B() : A(), Comparable<B> {
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_ERROR!>compareTo<!>(other: B): Int = 0
}

fun test() {
    B().compareTo(Any())
}
