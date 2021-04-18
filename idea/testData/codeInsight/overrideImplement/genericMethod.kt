// FIR_IDENTICAL
interface G<T> {
    fun foo(t : T) : T
}

class GC() : G<Int> {
    <caret>
}
