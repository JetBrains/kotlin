// RUN_PIPELINE_TILL: BACKEND
fun String.k(): () -> String = { -> this }

fun test() = "hello".k()()
