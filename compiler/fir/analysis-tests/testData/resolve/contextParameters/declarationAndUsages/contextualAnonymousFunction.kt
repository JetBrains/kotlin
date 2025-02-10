// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

annotation class Ann
class A

fun runWithA(block: context(String) () -> Unit) {
}

val t = context(a: A) fun () { a }
val t2 = @Ann context(a: A) fun () { a }

fun foo() {
    val t = context(a: A) fun () { a }
    val t2 = @Ann context(a: A) fun () { a }
    runWithA(context(a: String) fun () { a })
}