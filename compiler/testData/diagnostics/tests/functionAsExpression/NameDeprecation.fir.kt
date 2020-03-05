fun foo() {
    class A
    fun bar() {}
    (fun bar() {})
    fun A.foo() {}
    (fun A.foo() {})

    run(fun foo() {})
}
