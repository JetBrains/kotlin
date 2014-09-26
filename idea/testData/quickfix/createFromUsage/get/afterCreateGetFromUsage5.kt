// "Create function 'get' from usage" "true"
class Foo<T> {
    fun <T, V> x (y: Foo<Iterable<T>>, w: Iterable<V>) {
        val z: Iterable<T> = y["", w]
    }

    fun <V> get(s: String, w: Iterable<V>): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
