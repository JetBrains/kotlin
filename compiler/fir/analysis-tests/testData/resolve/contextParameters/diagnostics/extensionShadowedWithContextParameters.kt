// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class C {
    context(_: String)
    fun foo() {}

    fun bar() {}
}

fun C.foo() {}

context(_: String)
fun C.bar() {}
