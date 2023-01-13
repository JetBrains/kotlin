//KT-13330 AssertionError: Illegal resolved call to variable with invoke

fun foo(exec: (String.() -> Unit)?) = "".<!INAPPLICABLE_CANDIDATE!>exec<!><<!UNRESOLVED_REFERENCE!>caret<!>>() // <caret> is test data tag here
