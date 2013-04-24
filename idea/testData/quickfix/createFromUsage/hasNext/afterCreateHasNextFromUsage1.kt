// "Create method 'hasNext' from usage" "true"
class FooIterator<T> {
    fun next(): Int {
        throw Exception("not implemented")
    }
    fun hasNext(): Boolean {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
class Foo<T> {
    fun iterator(): FooIterator<String> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<Int>()) { }
}
