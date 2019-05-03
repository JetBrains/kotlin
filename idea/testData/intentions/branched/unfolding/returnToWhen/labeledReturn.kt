// WITH_RUNTIME
fun main() {
    run label@{
        <caret>return@label when {
            true ->
                42
            else -> 42
        }
    }
}