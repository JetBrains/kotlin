// PROBLEM: none
// WITH_RUNTIME

fun main() {
    listOf(42).map {
        it.also {
            <caret>it == 42
        }
    }
}