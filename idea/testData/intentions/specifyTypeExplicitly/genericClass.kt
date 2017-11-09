fun <T> generic1(): T = null!!

class My<T> {
    fun <caret>foo() = generic1<T>()
}