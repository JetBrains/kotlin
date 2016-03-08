// WITH_RUNTIME
fun foo(list: List<String?>): Int? {
    <caret>for (s in list) {
        if (s != null) {
            val length = s.length
            return length
        }
    }
    return null
}