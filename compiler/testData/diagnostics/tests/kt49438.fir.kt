fun <K> foo(x: K) {}
val x = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
