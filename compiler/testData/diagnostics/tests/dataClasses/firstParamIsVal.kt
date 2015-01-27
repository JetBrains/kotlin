data class A(val x: Int, <!UNUSED_PARAMETER!>y<!>: String)

fun foo(a: A) {
    a.component1() : Int
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
