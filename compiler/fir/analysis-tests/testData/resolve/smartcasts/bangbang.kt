// !DUMP_CFG
interface A {
    fun foo(): Boolean
}

fun test_0(a: A?) {
    a!!.foo()
    a.foo()
}

fun test_1(a: A?) {
    if (a!!.foo()) {
        a.foo()
    }
    a.foo()
}

fun test_2(a: A?, b: Boolean) {
    if (a!!.foo() && b) {
        a.foo()
    }
    a.foo()
}

fun test_3(a: A?, b: Boolean) {
    if (b && a!!.foo()) {
        a.foo() // OK
    }
    a.<!UNSAFE_CALL{LT}!><!UNSAFE_CALL{PSI}!>foo<!>()<!> // Bad
}

fun test_4(a: A?, b: Boolean) {
    if (a!!.foo() || b) {
        a.foo()
    }
    a.foo()
}

fun test_5(a: A?, b: Boolean) {
    if (b || a!!.foo()) {
        a.<!UNSAFE_CALL{LT}!><!UNSAFE_CALL{PSI}!>foo<!>()<!>
    }
    a.<!UNSAFE_CALL{LT}!><!UNSAFE_CALL{PSI}!>foo<!>()<!>
}

fun <X : A?> test_6(x: X) {
    x!!.foo()
}

fun <X : A> test_7(x: X?) {
    x!!.foo()
}
