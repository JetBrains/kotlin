fun foo() {
    class A
    <!CONFLICTING_OVERLOADS!>fun bar()<!> {}
    (<!CONFLICTING_OVERLOADS!>fun bar()<!> {})
    <!CONFLICTING_OVERLOADS!>fun A.foo()<!> {}
    (<!CONFLICTING_OVERLOADS!>fun A.foo()<!> {})

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>run<!>(<!ANONYMOUS_FUNCTION_WITH_NAME!>fun foo() {}<!>)
}
