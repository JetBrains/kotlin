// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.filterIndexed{}.firstOrNull()'"
fun foo(list: List<String>): Int? {
    <caret>for ((index, s) in list.withIndex()) {
        val l = s.length
        if (l > index) {
            return l
        }
    }
    return null
}