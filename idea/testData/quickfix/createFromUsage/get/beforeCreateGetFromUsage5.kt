// "Create function 'get' from usage" "true"
class Foo<T> {
    fun <T, V> x (y: Foo<Iterable<T>>, w: Iterable<V>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
