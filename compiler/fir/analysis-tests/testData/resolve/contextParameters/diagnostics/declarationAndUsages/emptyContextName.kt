// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

class B {
    fun foo(b: String): String { return b }
}

context(ctx: T)
fun <T> implicit(): T = ctx

context(_: A)
fun test1() {
    implicit<A>().foo("")
}

context(`_`: A)
fun test2() {
    implicit<A>().foo("")
}

context(_: A, b: B, `_`: Int)
fun test3() {
    implicit<A>().foo("")
    b.foo("")
    implicit<Int>().inc()
}

context(_: A)
val property1: String
    get() = implicit<A>().foo("")

context(`_`: A)
val property2: String
    get() = implicit<A>().foo("")

context(_: A, b: B, `_`: Int)
val property3: String
    get(){
        b.foo("")
        implicit<Int>().inc()
        return implicit<A>().foo("")
    }
