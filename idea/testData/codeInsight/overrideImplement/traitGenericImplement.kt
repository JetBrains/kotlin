// FIR_IDENTICAL
interface G<T> {
    fun foo(t : T) : T
}

class GC<T>() : G<T> {
    <caret>
}