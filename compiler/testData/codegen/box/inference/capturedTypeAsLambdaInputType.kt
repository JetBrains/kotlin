
class FooImpl<T : Any>(var value: T?) : Foo<T> {
    override fun update(transformer: (T?) -> T?) {
        value = transformer(value)
    }
}

interface Foo<T : Any> {
    fun update(transformer: (T?) -> T?)
}

fun Foo<*>.clear() {
    update { null }
}

fun box(): String {
    val foo = FooImpl(value = 1)
    foo.clear()
    return "OK"
}
