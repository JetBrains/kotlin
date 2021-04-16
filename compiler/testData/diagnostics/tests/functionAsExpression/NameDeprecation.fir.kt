fun foo() {
    class A
    fun bar() {}
    (fun bar() {})
    fun A.foo() {}
    (fun A.foo() {})

    run(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun foo() {}<!>)
}
