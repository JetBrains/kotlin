// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'firstOrNull{}'"
fun foo(list: List<String>) {
    var result: String? = null
    <caret>for (s in list) {
        if (s.length > 0) {
            result = s
            break
        }
    }
}