// !DIAGNOSTICS: -UNUSED_PARAMETER

class GenericClass<out T>(val value: T) {
    public fun <P> foo(extension: T.() -> P) {}
}

public fun <E> GenericClass<List<E>>.bar() {
    foo( { listIterator() })
}