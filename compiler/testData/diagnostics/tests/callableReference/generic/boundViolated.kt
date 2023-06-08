// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-56187

class Foo<T : Number> {
    val value: String = "OK"
    val genericValue: T = null!!
}

fun main() {
    val a = Foo<<!UPPER_BOUND_VIOLATED!>String<!>>::value
    val b = Foo<<!UPPER_BOUND_VIOLATED!>String<!>>::genericValue
}
