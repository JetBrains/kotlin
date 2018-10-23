fun invoke() {}

fun test1(a: dynamic) = a(1)
fun test1a(a: dynamic) = a.invoke(1)
fun test2(a: dynamic, b: dynamic) = a(b)
fun test2a(a: dynamic, b: dynamic) = a.invoke(b)
fun test2b(a: dynamic, b: dynamic) = a(b)(b)
fun test2c(a: dynamic, b: dynamic) = a(b).invoke(b)
fun test3(a: dynamic) = invoke()