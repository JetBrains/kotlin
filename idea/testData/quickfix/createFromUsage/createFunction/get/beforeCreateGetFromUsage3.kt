// "Create function 'get' from usage" "true"
class Foo<T> {
    fun <S> x (y: Foo<Iterable<S>>) {
        val z: Iterable<S> = y<caret>[""]
    }
}
