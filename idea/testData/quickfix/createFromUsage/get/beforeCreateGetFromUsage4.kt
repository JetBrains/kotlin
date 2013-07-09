// "Create function 'get' from usage" "true"
class Foo<T, S: Iterable<T>> {
    fun <U> x (y: Foo<U, Iterable<U>>) {
        val z: U = y<caret>[""]
    }
}
