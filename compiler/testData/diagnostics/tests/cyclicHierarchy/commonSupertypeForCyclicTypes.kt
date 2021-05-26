// FIR_IDENTICAL
open class A : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!>()
open class B : <!CYCLIC_INHERITANCE_HIERARCHY!>A<!>()

fun <T> select(vararg xs: T): T = xs[0]

fun foo() {
    select(A(), B())
}