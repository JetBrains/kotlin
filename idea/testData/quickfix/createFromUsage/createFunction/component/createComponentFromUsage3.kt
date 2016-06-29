// "Create extension function 'Any.component2'" "true"
// WITH_RUNTIME
class FooIterator<T> {
    operator fun hasNext(): Boolean { return false }
    operator fun next(): Any {
        throw UnsupportedOperationException("not implemented")
    }
}
class Foo<T> {
    operator fun iterator(): FooIterator<String> {
        throw UnsupportedOperationException("not implemented")
    }
}
operator fun Any.component1(): Int {
    return 0
}
fun foo() {
    for ((i: Int, j: Int) in Foo<caret><Int>()) { }
}