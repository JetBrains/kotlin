// "Create method 'set' from usage" "true"
class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        y["", w] = w
    }
    fun set(s: String, w: T, value: T) {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
