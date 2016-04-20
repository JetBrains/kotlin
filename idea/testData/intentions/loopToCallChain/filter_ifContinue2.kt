// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        if (!s.isEmpty()) continue
        return s
    }
    return null
}