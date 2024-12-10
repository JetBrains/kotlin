// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-73797

class A {
    fun foo(a: String): String { return a }
}

class B {
    fun bar(b: String): String { return b }
}

context(ctx: T)
fun <T> implicit(): T = ctx

context(_: A)
fun test1() {
    implicit<A>().foo("")
    <!UNRESOLVED_REFERENCE!>_<!>.foo("")
}

context(`_`: A)
fun test2() {
    implicit<A>().foo("")
    <!UNRESOLVED_REFERENCE!>`_`<!>.foo("")
}

context(_: A, b: B, _: Int)
fun test3() {
    implicit<A>().foo("")
    b.bar("")
    implicit<Int>().inc()
}

context(_: A)
val property1: String
    get() {
        <!UNRESOLVED_REFERENCE!>_<!>.foo("")
        return implicit<A>().foo("")
    }

context(`_`: A)
val property2: String
    get() {
        <!UNRESOLVED_REFERENCE!>`_`<!>.foo("")
        return implicit<A>().foo("")
    }

context(_: A, b: B, _: Int)
val property3: String
    get() {
        b.bar("")
        implicit<Int>().inc()
        return implicit<A>().foo("")
    }