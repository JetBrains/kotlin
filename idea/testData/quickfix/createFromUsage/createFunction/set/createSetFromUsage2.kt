// "Create extension function 'Any.set'" "true"
// WITH_RUNTIME

class Foo<T> {
    fun <T> x (y: Any, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
