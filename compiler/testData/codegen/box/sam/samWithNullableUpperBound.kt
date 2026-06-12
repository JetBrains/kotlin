// IGNORE_BACKEND: NATIVE
fun interface Consumer<T: Int?> {
    fun accept(u: T)
}

class K<T: Int?> {
    fun with(b: Consumer<T>) = b
}

fun id(x: Any?) = x

fun box(): String {
    val k: K<in Int?> = K()
    val consumer = k.with { x: Int? -> id(x) }
    consumer.accept(null)
    return "OK"
}
