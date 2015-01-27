data class A(<!UNUSED_PARAMETER!>x<!>: Int, var y: String)

fun foo(a: A) {
    a.component1() : String
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
