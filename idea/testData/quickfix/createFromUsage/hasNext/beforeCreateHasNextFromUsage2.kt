// "Create function 'hasNext' from usage" "true"
class FooIterator<T> {
    fun next(): T {
        throw Exception("not implemented")
    }
}
class Foo<T> {
    fun iterator(): FooIterator<T> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i in Foo<caret><Int>()) { }
}
