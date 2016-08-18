// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>, o: Any): Int? {
    <caret>for (s in list) {
        val length = s.length + (o as Int)
        if (length > 0) {
            val x = length * o
            return x
        }
    }
    return null
}