// "Create method 'get' from usage" "true"
class Foo<T> {
    fun <T, V> x (y: java.util.ArrayList<T>, w: java.util.ArrayList<V>) {
        val z: java.util.ArrayList<T>? = if (y<caret>["", w]) null else null
    }
}
