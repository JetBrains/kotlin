fun test1(a: Any, x: Collection<Any>) = a in x
fun test2(a: Any, x: Collection<Any>) = a !in x
fun <T> test3(a: T, x: Collection<T>) = a in x
fun <T> test4(a: T, x: Collection<T>) = a !in x