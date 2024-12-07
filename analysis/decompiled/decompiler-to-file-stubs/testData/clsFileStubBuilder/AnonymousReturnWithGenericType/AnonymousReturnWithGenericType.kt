// FIR_IDENTICAL
interface Foo<T>

class AnonymousReturnWithGenericType<T> {
    val v1 = object : Foo<T> {}
    fun f1() = object : Foo<T> {}

    private val v2 = object : Foo<T> {}
    private fun f2() = object : Foo<T> {}
}