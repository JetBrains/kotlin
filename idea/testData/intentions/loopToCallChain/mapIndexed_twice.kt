// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.mapIndexed{}.firstOrNull{}'"
fun foo(list: List<String>): Int? {
    <caret>for ((index, s) in list.withIndex()) {
        val x = s.length * index
        val y = x + index
        if (y > 0) return y
    }
    return null
}