// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

class Foo<T>
fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
    val z: Iterable<T> = y<caret>["", w]
}
