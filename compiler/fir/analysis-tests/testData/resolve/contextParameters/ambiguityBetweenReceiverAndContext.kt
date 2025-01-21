// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

context(ctx: T)
fun <T> implicit(): T = ctx

class A

context(a: A)
fun A.funMember() {
    implicit<A>()
}