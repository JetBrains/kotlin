fun main() {
    val randomFunction: kotlin.support.AbstractIterator<kotlin.Int>.(x: Int) -> Boolean = {<caret>y -> if (this.next() < y) true else false}
}

// WITH_RUNTIME