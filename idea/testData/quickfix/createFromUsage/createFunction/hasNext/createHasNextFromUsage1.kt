// "Create member function 'hasNext'" "true"
class FooIterator<T> {
    fun next(): Int {
        throw Exception("not implemented")
    }
}
class Foo<T> {
    fun iterator(): FooIterator<String> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
