// "Create method 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun <T, V> x (y: java.util.ArrayList<T>, w: java.util.ArrayList<V>) {
        val z: java.util.ArrayList<T>? = if (y["", w]) null else null
    }
}
fun <E, V> ArrayList<E>.get(s: String, w: ArrayList<V>): Boolean {
    throw UnsupportedOperationException("not implemented") //To change body of created methods use File | Settings | File Templates.
}