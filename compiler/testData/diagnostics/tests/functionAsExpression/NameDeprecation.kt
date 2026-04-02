// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    class A
    fun bar() {}
    (fun <!ANONYMOUS_FUNCTION_WITH_NAME!>bar<!>() {})
    fun A.foo() {}
    (fun A.<!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})

    <!CANNOT_INFER_PARAMETER_TYPE!>run<!>(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localClass, localFunction */
