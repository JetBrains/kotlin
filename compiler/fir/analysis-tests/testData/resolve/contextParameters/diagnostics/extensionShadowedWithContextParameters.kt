// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class C {
    context(_: String)
    fun foo() {}

    fun bar() {}
}

fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

context(_: String)
fun C.bar() {}
