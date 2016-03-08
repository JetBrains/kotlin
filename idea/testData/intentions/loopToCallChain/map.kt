// WITH_RUNTIME
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        val length = s.length
        if (length > 0) return length
    }
    return null
}