// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'none{}'"
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        if (s.length > 0) {
            return false
        }
    }
    return true
}