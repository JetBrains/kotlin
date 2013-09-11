trait A
trait B: A

trait Base<T>

fun <T> test(a: Base<B>) where T: Base<A> = a is <!CANNOT_CHECK_FOR_ERASED!>T<!>