// "Create function 'get' from usage" "true"
class Foo<T> {
    fun <S> x (y: Foo<Iterable<S>>) {
        val z: Iterable<S> = y[""]
    }
    fun get(s: String): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
