fun <T> foo1(x: T & Any) {}
fun <T> foo2(x: List<T & Any>) {}
fun <T> foo3(x: List<out T & Any>) {}
fun <T> foo4(x: (T & Any)) {}
fun <T> foo5(x: T? & Any?) {}
fun <T> foo6(x: T & Any & Any) {}
fun <T> foo7(x: T & (Any & Any)) {}
fun <T> T & Any.foo8() {}
fun <T> (T & Any).foo9() {}
fun <T> foo10(x: T & Any.() -> Unit) {}
fun <T> foo11(x: (T & Any).() -> Unit) {}
