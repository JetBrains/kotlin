// "Create function 'get' from usage" "true"
class Foo<T> {
    fun x (y: Foo<Iterable<T>>) {
        val z: Iterable<T> = y[""]
    }
    fun get(s: String): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
