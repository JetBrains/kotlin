// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T : Any> foo(x: T & Any, y: List<String & Any> & Any) {}

fun <F> bar1(x: F? & Any) {}
fun <F> bar2(x: F & Any?) {}
fun <F> bar3(x: (F?) & Any) {}
fun <F> bar4(x: (F & Any)?) {}

fun <F> bar5(x: F & String) {}

fun <F> bar6(x: F & (F & Any)) {}
fun <F> bar7(x: (F & Any) & Any) {}
fun <F> bar8(x: (F & Any).() -> Unit) {}
fun <F> (F & Any).bar9(x: () -> Unit) {}

fun <F> bar10(x: F & Any & String) {}
fun <F> bar11(x: Double & Any & String) {}
