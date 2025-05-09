// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-77417

class OutVariance<out T> {
    context(a: T)
    fun foo(b: <!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>) {}
}
