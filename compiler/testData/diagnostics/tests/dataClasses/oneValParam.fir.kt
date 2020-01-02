// !CHECK_TYPE

data class A(val x: Int)

fun foo(a: A) {
    checkSubtype<Int>(a.component1())
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
