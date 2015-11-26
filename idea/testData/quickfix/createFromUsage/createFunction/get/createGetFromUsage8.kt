// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

import java.util.ArrayList

class Foo<T> {
    fun x (y: Foo<List<T>>, w: ArrayList<T>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
