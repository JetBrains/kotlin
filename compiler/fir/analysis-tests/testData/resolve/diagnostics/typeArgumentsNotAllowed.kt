package rest

abstract class Foo<T> {
    abstract val x: <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Int><!>

    abstract fun foo(): <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<String><!>
}

fun <T> foo() {
    bar<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Int><!>>()
    bar<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Boolean><!>>>>()
}

fun <T> bar() {}

object Best {

}

val a = <!TYPE_ARGUMENTS_NOT_ALLOWED!>rest<Int><!>.<!UNRESOLVED_REFERENCE!>MyClass<!><String>
val b = Best.<!UNRESOLVED_REFERENCE!>MyClass<!><String>

class B<E>
class C<F<!SYNTAX!><<!><!SYNTAX!>Boolean<!><!SYNTAX!>><!><!SYNTAX!>><!> <!SYNTAX!>:<!> <!SYNTAX!>B<!><!SYNTAX!><<!><!SYNTAX!>F<!><!SYNTAX!><<!><!SYNTAX!>Boolean<!><!SYNTAX!>><!><!SYNTAX!>><!><!SYNTAX!>(<!><!SYNTAX!>)<!>

fun <G> gest() {}

fun <T> fest() {
    val b: List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Double><!>>
    gest<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Char><!>>()
    gest<T>()
    val c: List<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<String><!>>>>
    gest<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Boolean><!>>>>()
}
