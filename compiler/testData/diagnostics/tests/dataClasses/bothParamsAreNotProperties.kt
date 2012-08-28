data class A(x: Int, y: String)

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
