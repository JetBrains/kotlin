// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: T<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>) {}

trait A
class B<T: A>
fun <T> foo1(t: T<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><B<<!UPPER_BOUND_VIOLATED!>String<!>>><!>) {}