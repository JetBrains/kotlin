// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

context(ctx: A)
fun <A> implicit(): A = ctx

context(a: context(A)(String) -> String)
fun fun1() {
    a(A(), "O")
}

context(a: context(A)(String) -> String)
val prop1
    get() = { a(A(), "K") }

val x: context(A) (String) -> String = { y: String -> implicit<A>().foo(y) }

fun test() {
    with<context(A)(String) -> String, Unit>(x) {
        fun1()
        prop1()
    }
}