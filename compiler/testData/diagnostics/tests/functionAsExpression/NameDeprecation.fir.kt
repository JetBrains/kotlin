fun foo() {
    class A
    fun bar() {}
    (fun bar() {})
    fun A.foo() {}
    (fun A.foo() {})

    run(<!INFERENCE_ERROR!>fun foo() {}<!>)
}
