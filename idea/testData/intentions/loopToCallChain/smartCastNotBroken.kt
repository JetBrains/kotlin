// WITH_RUNTIME
fun foo(list: List<String>, o: Any): Int? {
    if (o is Int) {
        <caret>for (s in list) {
            val length = s.length + o
            if (length > 0) {
                val x = length * o.hashCode()
                return x
            }
        }
        return null
    }
    return 0
}