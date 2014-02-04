fun foo(i: Int) {
    <!FUNCTION_EXPECTED!>i<!>()
    <!CALLEE_NOT_A_FUNCTION!>1<!>()
}