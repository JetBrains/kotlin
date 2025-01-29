// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun test() {
    context(c: String)
    fun local() {}

    context(c: String) fun local2() {}
}