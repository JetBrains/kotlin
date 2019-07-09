// PROBLEM: none
// WITH_RUNTIME

fun main() {
    listOf(42).map {
        it.takeUnless {
            <caret>it == 42
        }
    }
}