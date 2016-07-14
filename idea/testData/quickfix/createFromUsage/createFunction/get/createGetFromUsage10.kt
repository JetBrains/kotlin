// "Create member function 'Foo.get'" "true"

class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
