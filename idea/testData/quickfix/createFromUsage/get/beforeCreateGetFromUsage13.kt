// "Create function 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun bar(arg: String) { }
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>) {
        val z = y<caret>["", w]
        bar(z)
    }
}
