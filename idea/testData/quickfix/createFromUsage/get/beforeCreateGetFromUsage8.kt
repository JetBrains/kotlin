// "Create method 'get' from usage" "true"
import java.util.ArrayList

class Foo<T> {
    fun x (y: Foo<List<T>>, w: ArrayList<T>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
