// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String?>): String? {
    <caret>for (s in list) {
        if (s == null || s.isNotEmpty()) {
            return s
        }
    }
    return ""
}