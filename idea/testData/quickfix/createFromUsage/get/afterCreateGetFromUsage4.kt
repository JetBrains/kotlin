// "Create method 'get' from usage" "true"
class Foo<T, S: Iterable<T>> {
    fun <U> x (y: Foo<U, Iterable<U>>) {
        val z: U = y[""]
    }
    fun get(s: String): T {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
