// PROBLEM: none
fun test(): String {
    <caret>try {
        return "success"
    } catch (e: Exception) {
        return "failure"
    } finally {
        return "finally"
    }
}