interface I

fun <E: I> foo(a: Any?): E? = a as? E

fun test() = foo<I>(null) ?: "OK"

fun box(): String = test().toString()
