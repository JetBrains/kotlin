// "Create method 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>) {
        val z: Iterable<T> = y["", w]
    }
    fun <V> get(s: String, w: ArrayList<V>): T {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
