// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(): Boolean {
    val foo = listOf(true, true)
    <caret>for (e in foo) {
        if (!e) return false
    }
    return true
}