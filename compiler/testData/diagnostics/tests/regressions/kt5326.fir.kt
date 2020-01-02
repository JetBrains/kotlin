class A<T> {
    fun size() = 0
}

class Foo<T>(i: Int)

public fun <E> Foo(c: A<E>) {
    val a = Foo<E>(c.size())       // Check no overload resolution ambiguity
    val b: Foo<E> = Foo(c.size())  // OK
    val c: Foo<Int> = Foo(c.size()) // OK
}