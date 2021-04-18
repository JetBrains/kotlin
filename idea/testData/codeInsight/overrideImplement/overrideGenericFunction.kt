// FIR_IDENTICAL
interface A<T> {
    fun foo(value : T) : Unit = println(value)
}

class C : A<C> {
    <caret>
}
