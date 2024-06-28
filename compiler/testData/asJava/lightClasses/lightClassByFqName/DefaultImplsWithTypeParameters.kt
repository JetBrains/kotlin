// Foo
interface Foo<X, Y> {
    fun <Z> foo(x: X, y: Y, z: Z) {}

    val x: Int get() = 0
}