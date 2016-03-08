// WITH_RUNTIME
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        if (s.isEmpty()) continue
        val l = s.length
        return l
    }
    return null
}