data class A<!DATA_CLASS_WITHOUT_PARAMETERS!>()<!>

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
