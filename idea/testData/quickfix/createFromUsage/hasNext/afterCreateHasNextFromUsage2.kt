// "Create method 'hasNext' from usage" "true"
class FooIterator<T> {
    fun next(): T {
        throw Exception("not implemented")
    }
    fun hasNext(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
class Foo<T> {
    fun iterator(): FooIterator<T> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i in Foo<Int>()) { }
}
