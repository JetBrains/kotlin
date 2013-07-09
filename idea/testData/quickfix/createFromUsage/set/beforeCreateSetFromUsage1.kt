// "Create function 'set' from usage" "true"
class Foo<T> {
    fun <T> x (y: Foo<List<T>>, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
