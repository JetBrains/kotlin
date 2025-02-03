// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

annotation class Ann
class A

val t = context(a: A) fun () { }
val t2 = @Ann context(a: A) fun () { }

fun foo() {
    val t = context(a: A) fun () { }
    val t2 = @Ann context(a: A) fun () { }
}