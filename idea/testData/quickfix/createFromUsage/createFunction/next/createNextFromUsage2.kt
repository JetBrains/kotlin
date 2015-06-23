// "Create member function 'next'" "true"
class FooIterator<T> {
    fun hasNext(): Boolean { return false }
}
class Foo<T> {
    fun iterator(): FooIterator<T> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
