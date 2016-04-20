// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIsInstance<>().map{}.firstOrNull()'"
fun foo(list: List<Any>): Int? {
    <caret>for (o in list) {
        if (o is String) {
            val length = o.length
            return length
        }
    }
    return null
}