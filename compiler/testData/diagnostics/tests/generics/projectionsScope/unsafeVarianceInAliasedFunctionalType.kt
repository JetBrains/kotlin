// FIR_IDENTICAL

class Foo<out T>(val baz: Baz<T>)

class Bar {
    val foo: Foo<*> = TODO()

    fun <T> bar(): Baz<T> {
        // behaviour different from FE1.0:
        // type(foo.baz) != Baz<T>
        return <!RETURN_TYPE_MISMATCH!>foo.baz<!>
    }
}

typealias Baz<T> = (@UnsafeVariance T) -> Unit