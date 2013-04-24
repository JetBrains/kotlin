// "Create method 'get' from usage" "true"
class Foo<T> {
    fun <S> x (y: Foo<Iterable<S>>) {
        val z: Iterable<S> = y[""]
    }
    fun get(s: String): T {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
