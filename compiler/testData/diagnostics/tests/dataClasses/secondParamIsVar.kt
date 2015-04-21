// !CHECK_TYPE

data class A(<!UNUSED_PARAMETER!>x<!>: Int, var y: String)

fun foo(a: A) {
    checkSubtype<String>(a.component1())
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
