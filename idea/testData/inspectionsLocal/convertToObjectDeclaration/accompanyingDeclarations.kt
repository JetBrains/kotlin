// PROBLEM: none
class <caret>A {
    companion object {
        const val name = "A"
    }
    val test = "B"
}

fun main(incoming: A) {
    val out = incoming.test
}