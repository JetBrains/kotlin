// FIR_IDENTICAL
interface I {
    fun z()
}

class C : A(), I {
    <caret>
}
