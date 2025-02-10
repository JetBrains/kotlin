// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
annotation class Ann

fun test() {
    context(c: String)
    fun local() {
        c
    }

    context(c: String) fun local2() {
        c
    }

    @Ann
    context(c: String)
    fun local3() {}
}