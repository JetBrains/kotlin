// "Create extension function 'set'" "true"
// ERROR: operator modifier is required on 'set' in ''

class Foo<T> {
    fun <T> x (y: Any, w: java.util.ArrayList<T>) {
        y<caret>["", w] = w
    }
}
