fun foo() {
    class A
    fun bar() {}
    (fun <!ANONYMOUS_FUNCTION_WITH_NAME!>bar<!>() {})
    fun A.foo() {}
    (fun A.<!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!>(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})
}
