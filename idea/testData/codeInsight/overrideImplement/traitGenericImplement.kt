trait G<T> {
    fun foo(t : T) : T
}

class GC<T>() : G<T> {
    <caret>
}