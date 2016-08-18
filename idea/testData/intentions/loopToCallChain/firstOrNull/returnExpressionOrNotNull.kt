// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int {
    <caret>for (s in list) {
        if (s.isNotEmpty()) {
            return s.length
        }
    }
    return -1
}