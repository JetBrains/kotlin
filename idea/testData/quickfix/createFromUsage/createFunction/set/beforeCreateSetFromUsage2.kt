// "Create extension function 'set'" "true"

class Foo<T> {
    fun <T> x (y: Any, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
