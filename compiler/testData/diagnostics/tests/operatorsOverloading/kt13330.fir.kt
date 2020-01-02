// !WITH_NEW_INFERENCE
//KT-13330 AssertionError: Illegal resolved call to variable with invoke

fun foo(exec: (String.() -> Unit)?) = "".<!INAPPLICABLE_CANDIDATE!>exec<!><caret>() // <caret> is test data tag here