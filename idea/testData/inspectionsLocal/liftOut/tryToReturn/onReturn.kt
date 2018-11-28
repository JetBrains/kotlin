// HIGHLIGHT: INFORMATION
fun test(): String {
    try {
        <caret>return "success"
    } catch (e: Exception) {
        throw e
    }
}