// PROBLEM: none
class Outer<T>() {
    inner class Inner

    fun test(inner: <caret>Outer<T>.Inner?) {
    }
}