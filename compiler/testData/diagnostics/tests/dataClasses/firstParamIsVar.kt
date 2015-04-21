// !CHECK_TYPE

data class A(var x: Int, <!UNUSED_PARAMETER!>y<!>: String)

fun foo(a: A) {
    checkSubtype<Int>(a.component1())
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}
