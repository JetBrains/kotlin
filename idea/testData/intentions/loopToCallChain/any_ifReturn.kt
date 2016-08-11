// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        if (s.length > 0) {
            return true
        }
    }
    return false
}