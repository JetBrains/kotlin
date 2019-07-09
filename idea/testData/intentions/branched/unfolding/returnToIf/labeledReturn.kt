// WITH_RUNTIME
fun main() {
    run label@{
        <caret>return@label if (true) {
            42
        } else {
            42
        }
    }
}