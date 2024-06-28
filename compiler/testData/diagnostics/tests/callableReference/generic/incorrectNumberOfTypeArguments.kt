// WITH_STDLIB
// ISSUE: KT-56186

class Foo<I, J : Number, K> {
    val value: String = "OK"
    val genericValue: Triple<I, J, K> = TODO()
}

fun test_1() {
    val a = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<!>::value
    val b = Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>value<!>
    val c = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Foo<!>::genericValue
    val d = Foo<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>genericValue<!>
}
