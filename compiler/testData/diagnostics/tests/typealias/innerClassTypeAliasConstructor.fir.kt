// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Outer {
    inner class Inner
}

typealias OI = Outer.Inner

fun test1(x: Outer) = x.<!UNRESOLVED_REFERENCE!>OI<!>()


class Generic<T> {
    inner class Inner
}

typealias GI<T> = Generic<T>.Inner
typealias GIntI = Generic<Int>.Inner

fun test2(x: Generic<Int>) = x.<!UNRESOLVED_REFERENCE!>GI<!>()
fun <T> test3(x: Generic<T>) = x.<!UNRESOLVED_REFERENCE!>GI<!>()
fun <T> test4(x: Generic<List<T>>) = x.<!UNRESOLVED_REFERENCE!>GI<!>()
fun <T> test5(x: Generic<T>) = x.<!UNRESOLVED_REFERENCE!>GIntI<!>()
fun Generic<Int>.test6() = GIntI()