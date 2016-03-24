// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<Any>, o: Any): Int? {
    <caret>for (s in list) {
        if (s is String && s.length > 0) {
            val x = s.length * 2
            return x
        }
    }
    return null
}