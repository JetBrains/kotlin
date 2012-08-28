data class A(x: Int, val y: String)

fun foo(a: A) {
    a.component1() : String
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
