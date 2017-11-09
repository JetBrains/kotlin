typealias Foo = Int

sealed class A
data class B(val foo: Int): A()

inline val f get() = ""

suspend fun test() {
    ""::class
    ""::toString

    Foo::class
    Foo::toString

    val b by lazy { "" }
}
