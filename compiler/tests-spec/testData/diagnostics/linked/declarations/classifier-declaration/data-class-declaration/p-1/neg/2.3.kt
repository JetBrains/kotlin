// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Non-property constructor parameters in the primary constructor after unreachable code
 */

// TESTCASE NUMBER: 1
fun case1(){
    <!UNREACHABLE_CODE!>val x =<!> TODO()
    data class A1(<!UNREACHABLE_CODE!>val x : Any<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, UNREACHABLE_CODE!>y: Any<!>)
}


// TESTCASE NUMBER: 2
fun case2(){
    <!UNREACHABLE_CODE!>val x =<!> TODO()
    data class A1(<!UNREACHABLE_CODE!>val x : Any<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER, UNREACHABLE_CODE!>vararg y: Any<!>)
}

// TESTCASE NUMBER: 3
fun case3(){
    <!UNREACHABLE_CODE!>val x =<!> TODO()
    data class A1(<!UNREACHABLE_CODE!>val x : Any<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, UNREACHABLE_CODE!>y: Any = 1<!>)
}


// TESTCASE NUMBER: 4
fun case4(){
    <!UNREACHABLE_CODE!>val x =<!> TODO()
    data class A1(<!UNREACHABLE_CODE!>val x : Any<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER, UNREACHABLE_CODE!>vararg y: Any = TODO()<!>)
}
