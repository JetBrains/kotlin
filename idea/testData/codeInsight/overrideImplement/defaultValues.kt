// FIR_IDENTICAL
interface T {
    fun foo(a:Int = 1)
}

class C : T {
    <caret>
}