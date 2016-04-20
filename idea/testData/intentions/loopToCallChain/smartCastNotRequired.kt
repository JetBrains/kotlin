// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '...filter{}.map{}.firstOrNull()'"
fun foo(list: List<String>, o: Any): Int? {
    <caret>for (s in list) {
        val length = s.length + (o as Int)
        if (length > 0) {
            val x = length * o.hashCode()
            return x
        }
    }
    return null
}