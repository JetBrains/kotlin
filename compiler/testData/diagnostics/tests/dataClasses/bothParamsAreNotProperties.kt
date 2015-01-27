data class A(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: String)

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
