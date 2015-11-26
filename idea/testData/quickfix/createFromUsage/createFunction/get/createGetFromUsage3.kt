// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

class Foo<T> {
    fun <S> x (y: Foo<Iterable<S>>) {
        val z: Iterable<S> = y<caret>[""]
    }
}
