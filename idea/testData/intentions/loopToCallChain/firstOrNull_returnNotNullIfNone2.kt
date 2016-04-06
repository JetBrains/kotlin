// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String?>): String? {
    <caret>for (s in list) {
        if (s == null || s.isNotEmpty()) {
            return s
        }
    }
    return ""
}