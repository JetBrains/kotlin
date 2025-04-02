// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-13712
// LANGUAGE: -ForbidOverloadClashesByErasure
// RENDER_DIAGNOSTICS_FULL_TEXT

open class A() {
    open fun compareTo(other: Any): Int = -1
}
class B() : A(), Comparable<B> {
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>compareTo<!>(other: B): Int = 0
}

fun test() {
    B().compareTo(Any())
}
