// IGNORE_LEAKED_INTERNAL_TYPES: KT-54568
fun <K> foo(x: K) {}
val x = foo<(<!UNRESOLVED_REFERENCE!>unresolved<!>) -> Float> { it.<!UNRESOLVED_REFERENCE!>toFloat<!>() }
