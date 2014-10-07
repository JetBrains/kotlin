// "Create function 'set' from usage" "true"
class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        y["", w] = w
    }

    fun set(s: String, w: T, value: T) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
