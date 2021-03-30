package rest

abstract class Foo<T> {
    abstract val x: <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Int><!>

    abstract fun foo(): <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<String><!>
}

fun <T> foo() {
    <!INAPPLICABLE_CANDIDATE!>bar<!><<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Int><!>>()
    bar<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Boolean><!>>>>()
}

fun <T> bar() {}

object Best {

}

val a = <!TYPE_ARGUMENTS_NOT_ALLOWED!>rest<Int><!>.<!UNRESOLVED_REFERENCE!>MyClass<!><String>
val b = Best.<!UNRESOLVED_REFERENCE!>MyClass<!><String>

class B<E>
class C<F<!SYNTAX{PSI}!><<!><!SYNTAX{PSI}!>Boolean<!><!SYNTAX{PSI}!>><!><!SYNTAX{PSI}!>><!> <!SYNTAX{PSI}!>:<!> <!SYNTAX{PSI}!>B<!><!SYNTAX{PSI}!><<!><!SYNTAX{PSI}!>F<!><!SYNTAX{PSI}!><<!><!SYNTAX{PSI}!>Boolean<!><!SYNTAX{PSI}!>><!><!SYNTAX{PSI}!>><!><!SYNTAX{PSI}!>(<!><!SYNTAX{PSI}!>)<!>

fun <G> gest() {}

fun <T> fest() {
    val b: List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Double><!>>
    <!INAPPLICABLE_CANDIDATE!>gest<!><<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Char><!>>()
    gest<T>()
    val c: List<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<String><!>>>>
    gest<List<List<<!TYPE_ARGUMENTS_NOT_ALLOWED!>T<Boolean><!>>>>()
}
