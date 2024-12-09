// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

class B {
    fun foo(a: String): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

context(c: A)
val a: context(B)(String) -> String
    get() = {
        c.foo("")
        implicit<B>().foo("")
    }

context(c: B)
val b: context(B)(String) -> String
    get() = {
        c.foo("")
        implicit<B>().foo("")
    }

fun test(){
    with(A()) {
        a(B(), "")
    }
    with(B()) {
        b(B(), "")
    }
}


