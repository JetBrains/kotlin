// PROBLEM: none
// WITH_RUNTIME

sealed class SC {
    <caret>class U : SC() {
        val a = mutableListOf<String>()
    }
}