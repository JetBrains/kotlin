fun foo() {
    class A
    fun bar() {}
    (fun bar() {})
    fun A.foo() {}
    (fun A.foo() {})

    run(<!EXPRESSION_EXPECTED!>fun foo() {}<!>)
}
