// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-75017

fun test() {
    context(c: String)
    fun local() {
        c
    }

    c

    fun local2() {
        c
    }
}