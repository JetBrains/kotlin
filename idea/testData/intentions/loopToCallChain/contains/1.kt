// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Boolean {
    <caret>for (s in list) {
        if (s == "a") {
            return true
        }
    }
    return false
}