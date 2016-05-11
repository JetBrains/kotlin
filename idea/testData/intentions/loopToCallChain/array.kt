// WITH_RUNTIME
fun foo(array: Array<String>): String? {
    <caret>for (s in array) {
        if (s.isNotBlank()) {
            return s
        }
    }
    return null
}
