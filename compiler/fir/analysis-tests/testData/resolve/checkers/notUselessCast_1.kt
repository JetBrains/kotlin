// ISSUE: KT-42136
// WITH_STDLIB

open class Base
class Derived : Base()

fun test_0(d: Derived) {
    fun take(d: Derived) {}
    take(d <!USELESS_CAST!>as Derived<!>) // should be USELESS_CAST
}

fun test_1() {
    val list = listOf<Any>().map { Derived() as Base }.toMutableList() // should be no USELESS_CAST
    list.add(Base())
}

fun test_2() {
    val list = listOf<Any>().map { Derived() }.toMutableList()
    list.add(<!ARGUMENT_TYPE_MISMATCH!>Base()<!>) // should be an error
}
