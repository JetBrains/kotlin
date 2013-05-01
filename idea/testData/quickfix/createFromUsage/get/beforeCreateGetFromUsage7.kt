// "Create function 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
