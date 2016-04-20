// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull()'"
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        return s
    }
    return null
}