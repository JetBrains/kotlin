// "Create function 'next' from usage" "true"
class FooIterator<T> {
    fun hasNext(): Boolean { return false }
    fun next(): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
class Foo<T> {
    fun iterator(): FooIterator<T> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<Int>()) { }
}
