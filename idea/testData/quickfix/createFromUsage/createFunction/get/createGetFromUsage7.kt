// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

import java.util.ArrayList

class Foo<T> {
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>, v: T) {
        val z: Iterable<T> = y<caret>["", w, v]
    }
}
