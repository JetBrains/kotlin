// "Create method 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun x (y: Foo<List<T>>, w: ArrayList<T>) {
        val z: Iterable<T> = y["", w]
    }
    fun get(s: String, w: T): T {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
