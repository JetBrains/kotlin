// FIR_IDENTICAL
// From KT-1254
interface T {
    fun Foo() : (String) -> Unit
}

class C : <caret>T