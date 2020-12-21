// !WITH_NEW_INFERENCE
//KT-13330 AssertionError: Illegal resolved call to variable with invoke

fun foo(exec: (String.() -> Unit)?) = "".<!UNSAFE_IMPLICIT_INVOKE_CALL, WRONG_NUMBER_OF_TYPE_ARGUMENTS{NI}!>exec<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS{OI}!><<!UNRESOLVED_REFERENCE!>caret<!>><!>() // <caret> is test data tag here
