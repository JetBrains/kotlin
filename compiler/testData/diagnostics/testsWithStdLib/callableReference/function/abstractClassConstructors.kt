// !DIAGNOSTICS: -UNUSED_EXPRESSION
trait A
abstract class B
annotation class C
enum class D

fun main() {
    ::<!UNRESOLVED_REFERENCE!>A<!>
    ::<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>B<!>
    ::C   // KT-3465
    ::<!INVISIBLE_MEMBER!>D<!>
}