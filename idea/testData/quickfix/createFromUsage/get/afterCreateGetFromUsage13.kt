// "Create method 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun bar(arg: String) { }
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>) {
        val z = y["", w]
        bar(z)
    }
    fun <V> get(s: String, w: ArrayList<V>): String {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
