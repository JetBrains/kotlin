// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class C {
    context(_: String)
    fun foo() {}

    fun bar() {}
}

fun C.foo() {}

context(_: String)
fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

context(_: String, _: Int)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>()<!> {}

context(_: String)
fun C.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext */
