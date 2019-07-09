// PROBLEM: none
// WITH_RUNTIME

fun main() {
    listOf(42).map {
        it.takeIf {
            <caret>it == 42
        }
    }
}