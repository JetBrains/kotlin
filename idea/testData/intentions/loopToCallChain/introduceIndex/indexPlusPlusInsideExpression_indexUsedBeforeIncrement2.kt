// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.mapIndexed{}.firstOrNull{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.mapIndexed{}.firstOrNull{}'"
fun foo(list: List<String>): Int? {
    var index = 0
    <caret>for (s in list) {
        val x = s.length * index
        val y = x * index++
        if (y > 1000) continue
        return y
    }
    return null
}