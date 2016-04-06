// WITH_RUNTIME
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        if (s.isNotEmpty()) {
            return s.length
        }
    }
    return null
}