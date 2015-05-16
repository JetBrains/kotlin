fun foo() {
    fun run(block: () -> Unit) = block()

    class A
    fun bar() {}
    (fun <!FUNCTION_EXPRESSION_WITH_NAME!>bar<!>() {})
    fun A.foo() {}
    (fun A.<!FUNCTION_EXPRESSION_WITH_NAME!>foo<!>() {})

    run(fun <!FUNCTION_EXPRESSION_WITH_NAME!>foo<!>() {})
}
