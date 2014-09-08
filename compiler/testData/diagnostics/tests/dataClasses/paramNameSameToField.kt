data class A(foo: String, val bar: Int, other: Long) {
    val foo = foo
    val other = other
}

fun test(a: A) {
    a.component1()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
    a.<!UNRESOLVED_REFERENCE!>component3<!>()
}