// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<String, Int><!>) {}

interface A
class B<T: A>
fun <T> foo1(t: <!TYPE_ARGUMENTS_NOT_ALLOWED!>T<B<String>><!>) {}