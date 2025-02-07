// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
annotation class Ann

fun test() {
    context(c: String)
    fun local() {}

    context(c: String) fun local2() {}

    @Ann
    context(c: String)
    fun local3() {}
}