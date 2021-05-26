fun foo() {
    class A
    fun bar() {}
    (fun bar() {})
    fun A.foo() {}
    (fun A.foo() {})

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!>(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun foo() {}<!>)
}
