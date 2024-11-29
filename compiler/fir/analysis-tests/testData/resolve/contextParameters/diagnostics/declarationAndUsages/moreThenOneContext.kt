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

context(a: A, b: B)
fun test1() {
    a.foo("")
    b.foo("")
}

context(a: A, b: B)
val property1: String
    get() {
        a.foo("")
        return b.foo("")
    }

fun inTypePosition(a: context(A, B) ()-> Unit){}

context(_: A, b: B)
fun test2(){
    implicit<A>().foo("")
    b.foo("")
}

context(_: A, b: B)
val property2: String
    get() {
        implicit<A>().foo("")
        return b.foo("")
    }

fun usage1() {
    with(A()){
        with(B()) {
            test1()
            test2()
            property1
            property2
        }
    }
    inTypePosition {
        implicit<A>().foo("")
        implicit<B>().foo("")
    }
}


