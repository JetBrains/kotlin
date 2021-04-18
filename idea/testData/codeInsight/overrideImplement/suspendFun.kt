// FIR_IDENTICAL
interface I {
    suspend fun foo()
}

class C : I {
    <caret>
}
