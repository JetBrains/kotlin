// "Create method 'get' from usage" "true"
class Foo<T> {
    fun x (y: Foo<Iterable<T>>) {
        val z: Iterable<T> = y<caret>[""]
    }
}
