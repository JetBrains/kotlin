// WITH_RUNTIME
fun `super`(x: Int): Int = TODO()

fun main(args: Array<String>) {
    listOf(1).map { <caret>`super`(it) }
}