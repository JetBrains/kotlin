// "Create function 'set' from usage" "true"

class Foo<T> {
    fun <T> x (y: Any, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
