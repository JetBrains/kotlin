// "Create function 'get' from usage" "true"
class Foo<T, S: Iterable<T>> {
    fun <U> x (y: Foo<U, Iterable<U>>) {
        val z: U = y[""]
    }

    fun get(s: String): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
