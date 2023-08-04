fun <K> foo(x: K) {}
val x1 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
val x2 = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
val x3 = foo<<!UNRESOLVED_REFERENCE!>unresolved<!>.() -> Float> { <!CANNOT_INFER_PARAMETER_TYPE!>this<!>.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
val x4 = foo<(Array<<!UNRESOLVED_REFERENCE!>unresolved<!>>) -> Int> { it.size }

fun <T> bar() = foo<(T) -> String> { it.toString() }
