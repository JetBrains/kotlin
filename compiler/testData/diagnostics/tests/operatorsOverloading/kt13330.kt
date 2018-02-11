// !WITH_NEW_INFERENCE
//KT-13330 AssertionError: Illegal resolved call to variable with invoke

fun foo(exec: (String.() -> Unit)?) = ""<!NI;UNSAFE_CALL!>.<!><!NI;WRONG_NUMBER_OF_TYPE_ARGUMENTS, OI;UNSAFE_IMPLICIT_INVOKE_CALL!>exec<!><!OI;WRONG_NUMBER_OF_TYPE_ARGUMENTS!><<!UNRESOLVED_REFERENCE!>caret<!>><!>() // <caret> is test data tag here