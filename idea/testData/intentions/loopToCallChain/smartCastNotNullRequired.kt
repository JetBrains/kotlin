// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>, o: Int?): Int? {
    <caret>for (s in list) {
        val length = s.length + o!!
        if (length > 0) {
            val x = length * o
            return x
        }
    }
    return null
}