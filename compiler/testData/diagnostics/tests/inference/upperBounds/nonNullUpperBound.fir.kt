fun <R : Any> unescape(value: Any): R? = throw Exception("$value")

fun <T: Any> foo(v: Any): T? = unescape(v)

//--------------

interface A

fun <R : A> unescapeA(value: Any): R? = throw Exception("$value")


fun <T: A> fooA(v: Any): T? = unescapeA(v)

