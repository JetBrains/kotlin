// "Create method 'get' from usage" "true"
class Foo<T> {
    fun x (y: Foo<Iterable<T>>) {
        val z: Iterable<T> = y[""]
    }
    fun get(s: String): T {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
