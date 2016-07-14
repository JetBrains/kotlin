// "Create member function 'Foo.set'" "true"

class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
