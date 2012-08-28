data class A(val x: Int)

fun foo(a: A) {
    a.component1() : Int
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
