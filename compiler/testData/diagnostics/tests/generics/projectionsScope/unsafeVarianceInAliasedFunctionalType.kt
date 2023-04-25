// FIR_IDENTICAL
// ISSUE: KT-54894
class Foo<out T>(val baz: Baz<T>)

class Bar {
    val foo: Foo<*> = TODO()

    fun <T> bar(): Baz<T> {
        return foo.baz
    }
}

typealias Baz<T> = (@UnsafeVariance T) -> Unit
