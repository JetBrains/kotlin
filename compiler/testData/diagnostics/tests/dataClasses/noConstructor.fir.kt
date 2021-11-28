data <!PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS!>class A<!>

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
