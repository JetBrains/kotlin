// HIGHLIGHT: INFORMATION

fun test(): String {
    <caret>try {
        return "success"
    } catch (e: Exception) {
        throw e
    }
}