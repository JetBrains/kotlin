// "Create function 'next' from usage" "true"
class FooIterator<T> {
    fun hasNext(): Boolean { return false }
    fun next(): Any {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
class Foo<T> {
    fun iterator(): FooIterator<String> {
        throw UnsupportedOperationException("not implemented")
    }
}
fun Any.component1(): Int {
    return 0
}
fun Any.component2(): Int {
    return 0
}
fun foo() {
    for ((i: Int, j: Int) in Foo<Int>()) { }
}