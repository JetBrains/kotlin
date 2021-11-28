package rest

abstract class Foo<T> {
    abstract val x: T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>

    abstract fun foo(): T<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>
}

fun <T> foo() {
    bar<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>>()
    bar<List<List<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Boolean><!>>>>()
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
    val b: List<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Double><!>>
    gest<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Char><!>>()
    gest<T>()
    val c: List<List<List<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>>>>
    gest<List<List<T<!TYPE_ARGUMENTS_NOT_ALLOWED!><Boolean><!>>>>()
}
