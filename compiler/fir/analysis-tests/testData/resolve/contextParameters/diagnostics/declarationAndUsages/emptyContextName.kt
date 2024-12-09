// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

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
}

context(`_`: A)
fun test2() {
    implicit<A>().foo("")
}

context(_: A, b: B, `_`: Int)
fun test3() {
    implicit<A>().foo("")
    b.bar("")
    implicit<Int>().inc()
}

context(_ : A, `_`: B)
fun test4() {
    <!UNRESOLVED_REFERENCE!>_<!>.foo("")
    <!UNRESOLVED_REFERENCE!>_<!>.bar("")
}

context(_: A)
val property1: String
    get() = implicit<A>().foo("")

context(`_`: A)
val property2: String
    get() = implicit<A>().foo("")

context(_: A, b: B, `_`: Int)
val property3: String
    get() {
        b.bar("")
        implicit<Int>().inc()
        return implicit<A>().foo("")
    }

context(_ : A, `_`: B)
val property4: String
    get() {
        <!UNRESOLVED_REFERENCE!>_<!>.foo("")
        <!UNRESOLVED_REFERENCE!>_<!>.bar("")
        return ""
    }
