// PROBLEM: none
// WITH_RUNTIME
fun test() {
    try {
    } catch (e: Exception) {
        <caret>if (e is RuntimeException) throw IllegalStateException(e)
    }
}