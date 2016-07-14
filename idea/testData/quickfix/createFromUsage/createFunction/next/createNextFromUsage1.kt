// "Create member function 'FooIterator.next'" "true"
class FooIterator<T> {
    operator fun hasNext(): Boolean { return false }
}
class Foo<T> {
    operator fun iterator(): FooIterator<String> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
