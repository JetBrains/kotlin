// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'flatMap{}.firstOrNull{}'"
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isNotBlank()) return line
        }
    }
    return null
}