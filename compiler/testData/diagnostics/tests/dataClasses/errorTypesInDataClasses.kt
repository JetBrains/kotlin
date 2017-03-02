// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val i: Int, val j: <!UNRESOLVED_REFERENCE!>G<!>)
data class B(val i: <!UNRESOLVED_REFERENCE!>G<!>, val j: <!UNRESOLVED_REFERENCE!>G<!>)


fun fa(a: A) {
    val (i, j) = a
    val i2 = a.component1()
    val j2 = a.component2()
}

fun fb(b: B) {
    val (i, j) = b
    val i2 = b.component1()
    val j2 = b.component2()
}