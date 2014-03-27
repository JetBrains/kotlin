fun main() {
    val randomFunction: kotlin.Array<kotlin.Int>.(x: Int) -> Boolean = {<caret>y -> if (this[0] < y) true else false}
}
