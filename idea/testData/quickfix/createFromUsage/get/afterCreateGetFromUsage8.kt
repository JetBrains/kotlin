// "Create function 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun x (y: Foo<List<T>>, w: ArrayList<T>) {
        val z: Iterable<T> = y["", w]
    }
    fun get(s: String, w: T): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
