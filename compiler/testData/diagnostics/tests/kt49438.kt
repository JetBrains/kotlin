fun <K> foo(x: K) {}
val x1 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toFloat<!>() }
val x2 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toFloat<!>() }
val x3 = foo<<!UNRESOLVED_REFERENCE!>unresolved<!>.() -> Float> { this.<!DEBUG_INFO_MISSING_UNRESOLVED!>toFloat<!>() }
val x4 = foo<(Array<<!UNRESOLVED_REFERENCE!>unresolved<!>>) -> Int> { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!>.size }

fun <T> bar() = foo<(T) -> String> { it.toString() }
