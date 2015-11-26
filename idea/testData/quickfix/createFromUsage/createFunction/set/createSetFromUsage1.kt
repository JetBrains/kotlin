// "Create member function 'set'" "true"
// ERROR: operator modifier is required on 'set' in 'Foo'

class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
