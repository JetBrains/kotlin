// FIR_IDENTICAL
interface Runnable {
    fun run()
}

class <caret>A : Runnable {
    fun foo() {
    }
}