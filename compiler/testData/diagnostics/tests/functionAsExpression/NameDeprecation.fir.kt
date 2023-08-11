fun foo() {
    class A
    fun bar() {}
    (<!ANONYMOUS_FUNCTION_WITH_NAME!>fun bar() {}<!>)
    fun A.foo() {}
    (<!ANONYMOUS_FUNCTION_WITH_NAME!>fun A.foo() {}<!>)

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!>(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun foo() {}<!>)
}
