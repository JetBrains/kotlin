interface I

fun <E: I> foo(a: Any?): E? = a as? E

fun box() = foo<I>(null) ?: "OK"