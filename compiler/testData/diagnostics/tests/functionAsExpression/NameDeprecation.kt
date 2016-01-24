fun foo() {
    class A
    fun bar() {}
    (fun <!ANONYMOUS_FUNCTION_WITH_NAME!>bar<!>() {})
    fun A.foo() {}
    (fun A.<!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})

    run(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() {})
}
