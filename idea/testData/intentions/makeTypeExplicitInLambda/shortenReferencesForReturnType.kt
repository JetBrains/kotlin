fun main(c: kotlin.support.AbstractIterator<Int>) {
    val f = { <caret>(x: Int) -> c}
}

// WITH_RUNTIME