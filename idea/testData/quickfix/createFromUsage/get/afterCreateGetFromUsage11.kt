// "Create function 'get' from usage" "true"
class Foo<T> {
    fun get(s: String, w: T): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
    val z: Iterable<T> = y["", w]
}
