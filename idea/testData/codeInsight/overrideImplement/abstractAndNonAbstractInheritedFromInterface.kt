// FIR_IDENTICAL
interface T {
    fun getFoo(): String = ""
}

interface U {
    fun getFoo(): String
}

class C1 : T, U {
    <caret>
}