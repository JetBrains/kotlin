// FIX: Replace 'it' with explicit parameter
// WITH_RUNTIME

fun main() {
    listOf(42).map {
        it == 42
        1.also {
            <caret>it == 42
        }
    }
}
