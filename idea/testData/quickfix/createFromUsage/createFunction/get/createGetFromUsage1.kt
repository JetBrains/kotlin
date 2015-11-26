// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

class Foo<T> {
    fun x (y: Foo<Iterable<T>>) {
        val z: Iterable<T> = y<caret>[""]
    }
}
