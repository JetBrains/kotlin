// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

import java.util.ArrayList

class Foo<T> {
    fun bar(arg: String) { }
    fun <T, V> x (y: Foo<List<T>>, w: ArrayList<V>) {
        val z = y<caret>["", w]
        bar(z)
    }
}
