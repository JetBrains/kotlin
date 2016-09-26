// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '...mapIndexed{}.mapIndexed{}.firstOrNull{}'"
fun foo(list: List<String>): Int? {
    var index = 0
    <caret>for (s in list) {
        if (s.isBlank()) continue
        val x = s.length * index
        val y = x * index++
        if (y > 1000) return y
    }
    return null
}