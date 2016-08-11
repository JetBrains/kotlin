// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): String? {
    var result: String? = null
    <caret>for (s in list) {
        for (line in s.lines()) {
            if (line.isNotBlank()) {
                result = line
                break
            }
        }
    }
    return result
}