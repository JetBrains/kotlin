// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class NestedContext {
    class B {
        fun bar() { }
    }
    companion object {
        fun bar() { }
    }
    object C {
        fun bar() { }
    }
    inner class D {
        fun bar() { }
    }

    fun bar() { }
}

context(ctx: T)
fun <T> implicit(): T = ctx

context(c: NestedContext.B)
fun test1() {
    c.bar()
    implicit<NestedContext.B>().bar()
    <!NO_CONTEXT_ARGUMENT!>implicit<!><NestedContext>().bar()
}

context(c: NestedContext.Companion)
fun test2() {
    c.bar()
    implicit<NestedContext.Companion>().bar()
    <!NO_CONTEXT_ARGUMENT!>implicit<!><NestedContext>().bar()
}

context(c: NestedContext.C)
fun test3() {
    c.bar()
    implicit<NestedContext.C>().bar()
    <!NO_CONTEXT_ARGUMENT!>implicit<!><NestedContext>().bar()
}

context(c: NestedContext.D)
fun test4() {
    c.bar()
    implicit<NestedContext.D>().bar()
    <!NO_CONTEXT_ARGUMENT!>implicit<!><NestedContext>().bar()
}

fun usage() {
    with(NestedContext.B()){
        test1()
    }
    with(NestedContext.Companion){
        test2()
    }
    with(NestedContext){
        test2()
    }
    with(NestedContext.C) {
        test3()
    }
    with(NestedContext().D()) {
        test4()
    }
    with(NestedContext()){
        <!NO_CONTEXT_ARGUMENT!>test1<!>()
        <!NO_CONTEXT_ARGUMENT!>test2<!>()
        <!NO_CONTEXT_ARGUMENT!>test3<!>()
        <!NO_CONTEXT_ARGUMENT!>test4<!>()
    }
}
