// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

context(c: A)
fun test() {
    c.foo("")
}

context(c: A)
var prop1: String
    get() = c.foo("")
    set(value) { c.foo("") }

fun usage() {
    <!NO_CONTEXT_ARGUMENT!>test<!>()
    <!NO_CONTEXT_ARGUMENT!>test<!>(<!TOO_MANY_ARGUMENTS!>A()<!>)
    <!NO_CONTEXT_ARGUMENT!>prop1<!>
}