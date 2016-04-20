// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.firstOrNull{}'"
fun foo(list: List<String>): Int? {
    <caret>for (s in list) {
        var length = s.length
        if (length > 0) return length
    }
    return null
}