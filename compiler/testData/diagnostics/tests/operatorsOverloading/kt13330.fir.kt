// !WITH_NEW_INFERENCE
//KT-13330 AssertionError: Illegal resolved call to variable with invoke

fun foo(exec: (String.() -> Unit)?) = "".<!UNRESOLVED_REFERENCE!>exec<!><caret>() // <caret> is test data tag here