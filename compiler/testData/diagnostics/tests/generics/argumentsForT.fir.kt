// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(t: T<String, Int>) {}

interface A
class B<T: A>
fun <T> foo1(t: T<B<String>>) {}