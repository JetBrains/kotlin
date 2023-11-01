data <!DATA_CLASS_WITHOUT_PARAMETERS!>class A<!>

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
