// WITH_STDLIB
// IGNORE_BACKEND: WASM_JS, WASM_WASI

class Key<T>(val s: String)
class Value<T>(val o: T)

interface Foo {
    fun <T> add(key: Key<T>, value: T)
    fun <T> remove(key: Key<T>): T?
}

class FooImpl : Foo {
    private val map = mutableMapOf<String, Value<*>>()

    override fun <T> add(key: Key<T>, value: T) {
        map[key.s] = Value(value)
    }

    override fun <T> remove(key: Key<T>): T? {
        val oldValue = map[key.s]
        map.remove(key.s)
        return (oldValue as Value<T>?)?.o
    }
}

fun box(): String {
    val foo = FooImpl()
    foo.add(Key<Boolean>("zzz"), true)
    foo.remove(Key<String>("zzz"))

    return "OK"
}