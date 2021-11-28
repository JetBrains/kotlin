typealias F<X> = (X?) -> X

fun <T> invoke(f: F<T>) = f(null)

fun box() = invoke<String> { it ?: "OK" }
