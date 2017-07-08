// HIGHLIGHT: GENERIC_ERROR_OR_WARNING

fun doSomething() {}

fun test(): String {
    <caret>try {
        doSomething()
        return "success"
    } catch (e: Exception) {
        doSomething()
        return "failure"
    }
}
