// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>): String? {
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isNotBlank() && line.length < s.length / 10) return line
        }
    }
    return null
}