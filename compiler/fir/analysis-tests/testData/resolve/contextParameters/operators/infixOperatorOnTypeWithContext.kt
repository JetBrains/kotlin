// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
class A {
    fun foo(a: String): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

infix fun (context(A)()->String).infixWithContext(a: Int) {}

infix fun Int.infixWithContextValue(a: context(A)()->String) {}

fun usage() {
    { a: A -> a.foo("") } infixWithContext 1
    { <!NO_CONTEXT_ARGUMENT!>implicit<!><A>().foo("") } <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>infixWithContext<!> 1

    1 infixWithContextValue { implicit<A>().foo("") }
}