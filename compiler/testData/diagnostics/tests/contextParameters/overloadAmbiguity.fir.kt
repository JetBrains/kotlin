// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(_: Any) fun f1() = 0
fun f1() = ""

context(_: Any) fun f2() {}
context(_: String) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun f2()<!> {}

context(_: Any) fun f3() {}
context(_: String, _: Any) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun f3()<!> {}

context(_: String)
fun test() {
    val x: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
