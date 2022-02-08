fun <T> foo1(x: @ann T & Any) {}
fun <T> foo2(x: List<@ann T & Any>) {}
fun <T> foo3(x: List<out @ann T & Any>) {}
fun <T> foo4(x: @ann (T & Any)) {}
fun <T> foo5(x: @ann T? & Any?) {}
fun <T> foo6(x: @ann T & Any & Any) {}
fun <T> foo7(x: @ann T & (Any & Any)) {}
fun <T> @ann (T & Any).foo8() {}
fun <T> foo10(x: @ann T & Any.() -> Unit) {}
fun <T> foo11(x: @ann (T & Any).() -> Unit) {}
