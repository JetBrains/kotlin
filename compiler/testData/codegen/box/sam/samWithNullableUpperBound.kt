// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3,2.4
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
