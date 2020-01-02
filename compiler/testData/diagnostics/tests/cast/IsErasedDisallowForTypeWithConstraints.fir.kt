interface A
interface B: A

interface Base<T>

fun <T> test(a: Base<B>) where T: Base<A> = a is T