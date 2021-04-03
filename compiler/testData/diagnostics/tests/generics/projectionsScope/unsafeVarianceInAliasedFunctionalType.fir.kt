class Foo<out T>(val baz: Baz<T>)

class Bar {
    val foo: Foo<*> = TODO()

    fun <T> bar(): Baz<T> {
        return <!RETURN_TYPE_MISMATCH!>foo.baz<!>
    }
}

typealias Baz<T> = (@UnsafeVariance T) -> Unit
